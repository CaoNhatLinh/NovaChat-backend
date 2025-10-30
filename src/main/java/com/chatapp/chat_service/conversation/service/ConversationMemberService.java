package com.chatapp.chat_service.conversation.service;
import com.chatapp.chat_service.auth.entity.User;
import com.chatapp.chat_service.auth.repository.UserRepository;
import com.chatapp.chat_service.common.exception.BadRequestException;
import com.chatapp.chat_service.common.exception.ForbiddenException;
import com.chatapp.chat_service.common.exception.NotFoundException;
import com.chatapp.chat_service.conversation.dto.ConversationMemberDto;
import com.chatapp.chat_service.conversation.entity.Conversation;
import com.chatapp.chat_service.conversation.entity.ConversationMembers;
import com.chatapp.chat_service.conversation.repository.ConversationMemberRepository;
import com.chatapp.chat_service.conversation.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationMemberService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationMemberService.class);
    
    private final ConversationMemberRepository memberRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    
    /**
     * Lấy danh sách members của conversation với thông tin chi tiết
     */
    public List<ConversationMemberDto> getConversationMembers(UUID conversationId) {
        List<ConversationMembers> members = memberRepository.findByKeyConversationId(conversationId);
        
        return members.stream()
                .map(this::toMemberDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Thêm members vào conversation
     * Chỉ owner và admin mới được thực hiện
     */
    @Transactional
    public void addMembers(UUID conversationId, List<UUID> memberIds, UUID requesterId) {
        // Kiểm tra conversation tồn tại
        Conversation conversation = getConversationOrThrow(conversationId);
        
        // Kiểm tra quyền
        checkPermission(conversationId, requesterId, List.of("owner", "admin"));
        
        // Thêm từng member
        Instant now = Instant.now();
        List<ConversationMembers> newMembers = memberIds.stream()
                .filter(memberId -> !isMemberOfConversation(conversationId, memberId))
                .map(memberId -> ConversationMembers.builder()
                        .key(new ConversationMembers.ConversationMemberKey(conversationId, memberId))
                        .role("member")
                        .joined_at(now)
                        .build())
                .collect(Collectors.toList());
        
        memberRepository.saveAll(newMembers);
        
        logger.info("Added {} members to conversation {} by user {}", 
                newMembers.size(), conversationId, requesterId);
    }
    
    /**
     * Kick member khỏi conversation
     * Chỉ owner và admin mới được thực hiện
     * Không thể kick owner
     */
    @Transactional
    public void removeMember(UUID conversationId, UUID memberIdToRemove, UUID requesterId) {
        // Kiểm tra conversation tồn tại
        Conversation conversation = getConversationOrThrow(conversationId);
        
        // Kiểm tra quyền
        checkPermission(conversationId, requesterId, List.of("owner", "admin"));
        
        // Lấy thông tin member cần kick
        ConversationMembers memberToRemove = getMemberOrThrow(conversationId, memberIdToRemove);
        
        // Không được kick owner
        if ("owner".equals(memberToRemove.getRole())) {
            throw new ForbiddenException("Không thể kick chủ phòng");
        }
        
        // Admin không được kick admin khác (chỉ owner mới được)
        String requesterRole = getMemberRole(conversationId, requesterId);
        if ("admin".equals(requesterRole) && "admin".equals(memberToRemove.getRole())) {
            throw new ForbiddenException("Admin không thể kick admin khác");
        }
        
        // Xóa member
        memberRepository.delete(memberToRemove);
        
        logger.info("Removed member {} from conversation {} by user {}", 
                memberIdToRemove, conversationId, requesterId);
    }
    
    /**
     * Chuyển quyền owner cho member khác
     * Chỉ owner hiện tại mới được thực hiện
     */
    @Transactional
    public void transferOwnership(UUID conversationId, UUID newOwnerId, UUID currentOwnerId) {
        // Kiểm tra conversation tồn tại
        Conversation conversation = getConversationOrThrow(conversationId);
        
        // Kiểm tra quyền (phải là owner)
        checkPermission(conversationId, currentOwnerId, List.of("owner"));
        
        // Kiểm tra new owner là member của conversation
        ConversationMembers newOwnerMember = getMemberOrThrow(conversationId, newOwnerId);
        ConversationMembers currentOwnerMember = getMemberOrThrow(conversationId, currentOwnerId);
        
        // Cập nhật role
        newOwnerMember.setRole("owner");
        currentOwnerMember.setRole("admin"); // Owner cũ trở thành admin
        
        memberRepository.save(newOwnerMember);
        memberRepository.save(currentOwnerMember);
        
        // Cập nhật created_by trong conversation
        conversation.setCreated_by(newOwnerId);
        conversation.setUpdated_at(Instant.now());
        conversationRepository.save(conversation);
        
        logger.info("Transferred ownership of conversation {} from {} to {}", 
                conversationId, currentOwnerId, newOwnerId);
    }
    
    /**
     * Trao quyền admin cho member
     * Chỉ owner mới được thực hiện
     */
    @Transactional
    public void grantAdmin(UUID conversationId, UUID userId, UUID ownerId) {
        // Kiểm tra conversation tồn tại
        Conversation conversation = getConversationOrThrow(conversationId);
        
        // Kiểm tra quyền (phải là owner)
        checkPermission(conversationId, ownerId, List.of("owner"));
        
        // Kiểm tra user là member
        ConversationMembers member = getMemberOrThrow(conversationId, userId);
        
        // Không thể trao quyền cho owner
        if ("owner".equals(member.getRole())) {
            throw new BadRequestException("Người dùng này đã là chủ phòng");
        }
        
        // Cập nhật role
        member.setRole("admin");
        memberRepository.save(member);
        
        logger.info("Granted admin role to user {} in conversation {} by owner {}", 
                userId, conversationId, ownerId);
    }
    
    /**
     * Thu hồi quyền admin
     * Chỉ owner mới được thực hiện
     */
    @Transactional
    public void revokeAdmin(UUID conversationId, UUID userId, UUID ownerId) {
        // Kiểm tra conversation tồn tại
        Conversation conversation = getConversationOrThrow(conversationId);
        
        // Kiểm tra quyền (phải là owner)
        checkPermission(conversationId, ownerId, List.of("owner"));
        
        // Kiểm tra user là member
        ConversationMembers member = getMemberOrThrow(conversationId, userId);
        
        if (!"admin".equals(member.getRole())) {
            throw new BadRequestException("Người dùng này không phải là admin");
        }
        
        // Cập nhật role
        member.setRole("member");
        memberRepository.save(member);
        
        logger.info("Revoked admin role from user {} in conversation {} by owner {}", 
                userId, conversationId, ownerId);
    }
    
    /**
     * Rời khỏi conversation
     * Owner không được phép rời (phải chuyển quyền trước)
     */
    @Transactional
    public void leaveConversation(UUID conversationId, UUID userId) {
        // Kiểm tra conversation tồn tại
        Conversation conversation = getConversationOrThrow(conversationId);
        
        // Kiểm tra user là member
        ConversationMembers member = getMemberOrThrow(conversationId, userId);
        
        // Owner không được rời
        if ("owner".equals(member.getRole())) {
            throw new ForbiddenException("Chủ phòng không thể rời khỏi phòng. Vui lòng chuyển quyền chủ phòng trước.");
        }
        
        // Xóa member
        memberRepository.delete(member);
        
        logger.info("User {} left conversation {}", userId, conversationId);
    }
    
    /**
     * Kiểm tra user có phải là member của conversation không
     */
    public boolean isMemberOfConversation(UUID conversationId, UUID userId) {
        return memberRepository.findById(
                new ConversationMembers.ConversationMemberKey(conversationId, userId)
        ).isPresent();
    }
    
    /**
     * Lấy role của user trong conversation
     */
    public String getMemberRole(UUID conversationId, UUID userId) {
        return memberRepository.findById(
                new ConversationMembers.ConversationMemberKey(conversationId, userId)
        ).map(ConversationMembers::getRole)
         .orElse(null);
    }
    
    // Helper methods
    
    private Conversation getConversationOrThrow(UUID conversationId) {
        return conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation không tồn tại"));
    }
    
    private ConversationMembers getMemberOrThrow(UUID conversationId, UUID userId) {
        return memberRepository.findById(
                new ConversationMembers.ConversationMemberKey(conversationId, userId)
        ).orElseThrow(() -> new NotFoundException("User không phải là member của conversation này"));
    }
    
    private void checkPermission(UUID conversationId, UUID userId, List<String> allowedRoles) {
        String role = getMemberRole(conversationId, userId);
        
        if (role == null) {
            throw new ForbiddenException("Bạn không phải là member của conversation này");
        }
        
        if (!allowedRoles.contains(role)) {
            throw new ForbiddenException("Bạn không có quyền thực hiện hành động này");
        }
    }
    
    private ConversationMemberDto toMemberDto(ConversationMembers member) {
        UUID userId = member.getUserId();
        
        // Lấy thông tin user
        Optional<User> userOpt = userRepository.findById(userId);
        
        ConversationMemberDto.ConversationMemberDtoBuilder builder = ConversationMemberDto.builder()
                .userId(userId)
                .conversationId(member.getConversationId())
                .role(member.getRole())
                .joinedAt(member.getJoined_at());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            builder.username(user.getUsername())
                   .displayName(user.getDisplay_name())
                   .avatarUrl(user.getAvatar_url());
        }
        
        // Set default online status
        builder.isOnline(false);
        
        return builder.build();
    }
}
