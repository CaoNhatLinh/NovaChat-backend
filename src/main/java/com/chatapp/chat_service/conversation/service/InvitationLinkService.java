package com.chatapp.chat_service.conversation.service;

import com.chatapp.chat_service.auth.entity.User;
import com.chatapp.chat_service.auth.repository.UserRepository;
import com.chatapp.chat_service.common.exception.BadRequestException;
import com.chatapp.chat_service.common.exception.ForbiddenException;
import com.chatapp.chat_service.common.exception.NotFoundException;
import com.chatapp.chat_service.conversation.dto.InvitationLinkDto;
import com.chatapp.chat_service.conversation.entity.ConversationMembers;
import com.chatapp.chat_service.conversation.entity.InvitationLink;
import com.chatapp.chat_service.conversation.repository.ConversationMemberRepository;
import com.chatapp.chat_service.conversation.repository.InvitationLinkRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationLinkService {
    private static final Logger logger = LoggerFactory.getLogger(InvitationLinkService.class);
    
    private final InvitationLinkRepository invitationLinkRepository;
    private final ConversationMemberRepository memberRepository;
    private final ConversationMemberService conversationMemberService;
    private final UserRepository userRepository;
    
    @Value("${app.invitation.base-url:http://localhost:3000/join}")
    private String invitationBaseUrl;
    
    /**
     * Tạo invitation link mới
     * Owner và admin mới được tạo
     */
    @Transactional
    public InvitationLinkDto createInvitationLink(UUID conversationId, UUID creatorId, Long expiresInHours, Integer maxUses) {
        // Kiểm tra quyền
        String role = conversationMemberService.getMemberRole(conversationId, creatorId);
        if (role == null || (!role.equals("owner") && !role.equals("admin"))) {
            throw new ForbiddenException("Chỉ chủ phòng và admin mới được tạo link mời");
        }
        
        // Tạo link token duy nhất
        String linkToken = UUID.randomUUID().toString().replace("-", "");
        
        // Tính thời gian hết hạn
        Instant expiresAt = Instant.now().plusSeconds((expiresInHours != null ? expiresInHours : 24) * 3600);
        
        InvitationLink link = InvitationLink.builder()
                .linkId(UUID.randomUUID())
                .conversationId(conversationId)
                .linkToken(linkToken)
                .createdBy(creatorId)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .isActive(true)
                .maxUses(maxUses)
                .usedCount(0)
                .build();
        
        invitationLinkRepository.save(link);
        
        logger.info("Created invitation link {} for conversation {} by user {}", 
                link.getLinkId(), conversationId, creatorId);
        
        return toLinkDto(link, creatorId);
    }
    
    /**
     * Lấy danh sách invitation links của conversation
     */
    public List<InvitationLinkDto> getConversationLinks(UUID conversationId, UUID requesterId) {
        // Kiểm tra user là member
        if (!conversationMemberService.isMemberOfConversation(conversationId, requesterId)) {
            throw new ForbiddenException("Bạn không phải là member của conversation này");
        }
        
        List<InvitationLink> links = invitationLinkRepository.findByConversationId(conversationId);
        
        return links.stream()
                .map(link -> toLinkDto(link, requesterId))
                .collect(Collectors.toList());
    }
    
    /**
     * Xóa invitation link
     * Owner, admin hoặc người tạo link mới được xóa
     */
    @Transactional
    public void deleteInvitationLink(UUID linkId, UUID requesterId) {
        InvitationLink link = invitationLinkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Link không tồn tại"));
        
        String role = conversationMemberService.getMemberRole(link.getConversationId(), requesterId);
        
        // Kiểm tra quyền: owner, admin hoặc người tạo link
        boolean isCreator = link.getCreatedBy().equals(requesterId);
        boolean isOwnerOrAdmin = role != null && (role.equals("owner") || role.equals("admin"));
        
        if (!isCreator && !isOwnerOrAdmin) {
            throw new ForbiddenException("Bạn không có quyền xóa link này");
        }
        
        invitationLinkRepository.delete(link);
        
        logger.info("Deleted invitation link {} by user {}", linkId, requesterId);
    }
    
    /**
     * Vô hiệu hóa link
     */
    @Transactional
    public void deactivateLink(UUID linkId, UUID requesterId) {
        InvitationLink link = invitationLinkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Link không tồn tại"));
        
        String role = conversationMemberService.getMemberRole(link.getConversationId(), requesterId);
        
        // Kiểm tra quyền
        boolean isCreator = link.getCreatedBy().equals(requesterId);
        boolean isOwnerOrAdmin = role != null && (role.equals("owner") || role.equals("admin"));
        
        if (!isCreator && !isOwnerOrAdmin) {
            throw new ForbiddenException("Bạn không có quyền vô hiệu hóa link này");
        }
        
        link.setActive(false);
        invitationLinkRepository.save(link);
        
        logger.info("Deactivated invitation link {} by user {}", linkId, requesterId);
    }
    
    /**
     * Join conversation qua invitation link
     */
    @Transactional
    public void joinViaInvitationLink(String linkToken, UUID userId) {
        InvitationLink link = invitationLinkRepository.findByLinkToken(linkToken)
                .orElseThrow(() -> new NotFoundException("Link không tồn tại hoặc đã hết hạn"));
        
        // Validate link
        validateLink(link);
        
        // Kiểm tra user đã là member chưa
        if (conversationMemberService.isMemberOfConversation(link.getConversationId(), userId)) {
            throw new BadRequestException("Bạn đã là thành viên của phòng này");
        }
        
        // Thêm user vào conversation
        ConversationMembers newMember = ConversationMembers.builder()
                .key(new ConversationMembers.ConversationMemberKey(link.getConversationId(), userId))
                .role("member")
                .joined_at(Instant.now())
                .build();
        
        memberRepository.save(newMember);
        
        // Tăng số lần sử dụng
        link.setUsedCount(link.getUsedCount() + 1);
        
        // Nếu đã đạt maxUses, vô hiệu hóa link
        if (link.getMaxUses() != null && link.getUsedCount() >= link.getMaxUses()) {
            link.setActive(false);
        }
        
        invitationLinkRepository.save(link);
        
        logger.info("User {} joined conversation {} via invitation link {}", 
                userId, link.getConversationId(), linkToken);
    }
    
    /**
     * Validate invitation link
     */
    private void validateLink(InvitationLink link) {
        if (!link.isActive()) {
            throw new BadRequestException("Link đã bị vô hiệu hóa");
        }
        
        if (link.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Link đã hết hạn");
        }
        
        if (link.getMaxUses() != null && link.getUsedCount() >= link.getMaxUses()) {
            throw new BadRequestException("Link đã đạt giới hạn số lần sử dụng");
        }
    }
    
    /**
     * Convert entity to DTO
     */
    private InvitationLinkDto toLinkDto(InvitationLink link, UUID requesterId) {
        String role = conversationMemberService.getMemberRole(link.getConversationId(), requesterId);
        boolean isCreator = link.getCreatedBy().equals(requesterId);
        boolean isOwnerOrAdmin = role != null && (role.equals("owner") || role.equals("admin"));
        
        // Lấy username của người tạo
        String createdByUsername = userRepository.findById(link.getCreatedBy())
                .map(User::getUsername)
                .orElse("Unknown");
        
        return InvitationLinkDto.builder()
                .linkId(link.getLinkId())
                .conversationId(link.getConversationId())
                .linkToken(link.getLinkToken())
                .fullLink(invitationBaseUrl + "/" + link.getLinkToken())
                .createdBy(link.getCreatedBy())
                .createdByUsername(createdByUsername)
                .createdAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .isActive(link.isActive())
                .maxUses(link.getMaxUses())
                .usedCount(link.getUsedCount())
                .isExpired(link.getExpiresAt().isBefore(Instant.now()))
                .canDelete(isCreator || isOwnerOrAdmin)
                .build();
    }
}
