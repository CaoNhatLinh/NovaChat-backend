package com.chatapp.chat_service.conversation.service;

import com.chatapp.chat_service.redis.publisher.RedisCacheEvictPublisher;
import com.chatapp.chat_service.elasticsearch.service.ConversationElasticsearchService;
import com.chatapp.chat_service.auth.service.UserService;
import com.chatapp.chat_service.common.exception.BadRequestException;
import com.chatapp.chat_service.conversation.dto.ConversationRequest;
import com.chatapp.chat_service.conversation.dto.ConversationResponseDto;
import com.chatapp.chat_service.conversation.dto.ConversationSearchDto;
import com.chatapp.chat_service.conversation.entity.Conversation;
import com.chatapp.chat_service.conversation.entity.ConversationMembers;
import com.chatapp.chat_service.conversation.exception.ConversationAlreadyExistsException;
import com.chatapp.chat_service.conversation.repository.ConversationMemberRepository;
import com.chatapp.chat_service.conversation.repository.ConversationRepository;
import com.chatapp.chat_service.elasticsearch.document.ConversationDocument;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service

public class ConversationService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final RedisCacheEvictPublisher cacheEvictPublisher;
    
    @Autowired(required = false)
    private ConversationElasticsearchService conversationElasticsearchService; // Optional Elasticsearch service

    @Autowired
    private UserService userService;

    public ConversationService(RedisTemplate<String, Object> redisTemplate, ConversationRepository conversationRepository, ConversationMemberRepository memberRepository, RedisCacheEvictPublisher cacheEvictPublisher) {
        this.redisTemplate = redisTemplate;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.cacheEvictPublisher = cacheEvictPublisher;
    }
    public List<Conversation> getUserConversations(UUID userId) {
        // Lấy tất cả dòng từ bảng conversation_members mà user là thành viên
        List<ConversationMembers> memberships = memberRepository.findByKeyUserId(userId);

        if (memberships.isEmpty()) return List.of();

        // Lấy danh sách conversation_id từ các dòng trên
        List<UUID> conversationIds = memberships.stream()
                .map(m -> m.getKey().getConversationId())
                .distinct() // tránh trùng
                .toList();

        // Query each conversation individually and filter out deleted ones
        List<Conversation> conversations = new ArrayList<>();
        for (UUID conversationId : conversationIds) {
            Optional<Conversation> conversation = conversationRepository.findByConversationId(conversationId);
            if (conversation.isPresent() && !conversation.get().is_deleted()) {
                conversations.add(conversation.get());
            }
        }
        
        return conversations;
    }


    public Conversation createConversation(ConversationRequest req, UUID createdId) {
        // Kiểm tra nếu là DM conversation và đã tồn tại
        if ("dm".equals(req.getType())) {
            if (req.getMemberIds().size() != 1) {
                throw new BadRequestException("DM conversation chỉ được có đúng 2 thành viên (creator + 1 member)");
            }
            
            UUID otherUserId = req.getMemberIds().get(0);
            
            // Kiểm tra không thể tạo DM với chính mình
            if (createdId.equals(otherUserId)) {
                throw new BadRequestException("Không thể tạo phòng chat riêng với chính mình");
            }
            
            Optional<Conversation> existingDM = findPrivateConversationWithCache(createdId, otherUserId);
            
            if (existingDM.isPresent()) {
                throw new ConversationAlreadyExistsException("Phòng chat riêng giữa hai người này đã tồn tại và chưa bị xóa. Không thể tạo phòng mới.");
            }
        }
        
        UUID conversationId = UUID.randomUUID();
        Instant now = Instant.now();

        Conversation conversation = Conversation.builder()
                .conversationId(conversationId)
                .type(req.getType())
                .name(req.getName())
                .description(req.getDescription())
                .is_deleted(false)
                .created_by(createdId)
                .created_at(now)
                .updated_at(now)
                .build();

        conversationRepository.save(conversation);

        // Thêm creator + thành viên
        Set<UUID> allMembers = new HashSet<>(req.getMemberIds());
        allMembers.add(createdId);
        List<ConversationMembers> members = allMembers.stream()
                .map(userId -> ConversationMembers.builder()
                        .key(new ConversationMembers.ConversationMemberKey(conversationId, userId))
                        .role(userId.equals(createdId) ? "owner" : "member")
                        .joined_at(now)
                        .build())
                .toList();

        memberRepository.saveAll(members);
        
        // Index to Elasticsearch
        indexConversationToElasticsearch(conversation);
        
        if ("dm".equals(req.getType()) && allMembers.size() == 2) {
            cachePrivateConversation(allMembers, conversation);
        }

        logger.info("Created {} conversation {} by user {}", req.getType(), conversationId, createdId);
        return conversation;
    }
    private void cachePrivateConversation(Set<UUID> memberIds, Conversation conversation) {
        // Chuyển Set thành List để lấy 2 user
        List<UUID> sortedUserIds = new ArrayList<>(memberIds);
        Collections.sort(sortedUserIds); // Đảm bảo thứ tự user1 < user2

        UUID user1 = sortedUserIds.get(0);
        UUID user2 = sortedUserIds.get(1);
        String cacheKey = String.format(
                "dmChat:%s:%s",
                sortedUserIds.get(0),
                sortedUserIds.get(1)
        );

        // Lưu vào cache
        redisTemplate.opsForValue().set(
                cacheKey,
                conversation,
                Duration.ofHours(1) // TTL 1 giờ
        );
        cacheEvictPublisher.publish(cacheKey); // Nếu muốn thông báo thay vì xóa
    }
    public Optional<Conversation> getConversationById(UUID conversationId) {
        Optional<Conversation> conversation = conversationRepository.findByConversationId(conversationId);
        if (conversation.isPresent() && !conversation.get().is_deleted()) {
            return conversation;
        }
        return Optional.empty();
    }

    public Optional<Conversation> findPrivateConversation(UUID userId1, UUID userId2) {
        List<UUID> user1Conversations = memberRepository.findConversationIdsByUserId(userId1);
        List<UUID> user2Conversations = memberRepository.findConversationIdsByUserId(userId2);

        Set<UUID> commonConversations = new HashSet<>(user1Conversations);
        commonConversations.retainAll(user2Conversations);

        for (UUID conversationId : commonConversations) {
            // Tìm conversation với điều kiện is_deleted = false và type = "dm"
            Optional<Conversation> conversation = conversationRepository.findByConversationId(conversationId);
            if (conversation.isPresent() && !conversation.get().is_deleted() && "dm".equals(conversation.get().getType())) {
                return conversation;
            }
        }
        return Optional.empty();
    }



    /**
     * Soft delete một conversation bằng cách set is_deleted = true
     */
    public boolean deleteConversation(UUID conversationId, UUID userId) {
        Optional<Conversation> conversationOpt = conversationRepository.findByConversationId(conversationId);
        
        if (conversationOpt.isEmpty() || conversationOpt.get().is_deleted()) {
            return false; // Conversation không tồn tại hoặc đã bị xóa
        }
        
        Conversation conversation = conversationOpt.get();
        
        // Kiểm tra quyền xóa (chỉ owner hoặc admin mới được xóa)
        if (!conversation.getCreated_by().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa conversation này");
        }
        
        // Soft delete
        conversation.set_deleted(true);
        conversation.setUpdated_at(Instant.now());
        
        conversationRepository.save(conversation);
        
        // Update Elasticsearch if available
        if (conversationElasticsearchService != null) {
            conversationElasticsearchService.deleteConversation(conversationId);
        }
        
        // Xóa cache conversation thông thường
        String cacheKey = "conversation:" + conversationId;
        redisTemplate.delete(cacheKey);
        
        // Nếu là DM conversation, xóa cache DM
        if ("dm".equals(conversation.getType())) {
            clearDmCache(conversationId);
        }
        
        return true;
    }
    
    /**
     * Khôi phục một conversation đã bị soft delete
     */
    public boolean restoreConversation(UUID conversationId, UUID userId) {
        Optional<Conversation> conversationOpt = conversationRepository.findByConversationId(conversationId);
        
        if (conversationOpt.isEmpty()) {
            return false;
        }
        
        Conversation conversation = conversationOpt.get();
        
        // Kiểm tra quyền khôi phục
        if (!conversation.getCreated_by().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền khôi phục conversation này");
        }
        
        // Khôi phục
        conversation.set_deleted(false);
        conversation.setUpdated_at(Instant.now());
        
        conversationRepository.save(conversation);
        
        // Update Elasticsearch if available
        if (conversationElasticsearchService != null) {
            conversationElasticsearchService.restoreConversation(conversationId);
        }
        
        return true;
    }
    /**
     * Tìm phòng chat private giữa 2 user với cache Redis
     * Sử dụng cache để tối ưu performance
     */
    public Optional<Conversation> findPrivateConversationWithCache(UUID userId1, UUID userId2) {
        // Đảm bảo thứ tự userId để cache key nhất quán
        List<UUID> sortedUserIds = Arrays.asList(userId1, userId2);
        Collections.sort(sortedUserIds);
        
        UUID user1 = sortedUserIds.get(0);
        UUID user2 = sortedUserIds.get(1);
        
        // Kiểm tra cache trước
        String cacheKey = String.format("dmChat:%s:%s", user1, user2);
        try {
            Object cachedConversation = redisTemplate.opsForValue().get(cacheKey);
            if (cachedConversation instanceof Conversation) {
                Conversation conversation = (Conversation) cachedConversation;
                // Kiểm tra conversation từ cache có bị xóa không
                if (!conversation.is_deleted()) {
                    return Optional.of(conversation);
                } else {
                    // Xóa cache nếu conversation đã bị xóa
                    redisTemplate.delete(cacheKey);
                }
            }
        } catch (Exception e) {
            // Nếu có lỗi cache, tiếp tục query database
            logger.warn("Error accessing cache for DM conversation: {}", e.getMessage());
        }
        
        // Nếu không có trong cache hoặc cache đã hết hạn, query database
        Optional<Conversation> conversation = findPrivateConversation(userId1, userId2);
        
        // Cache lại kết quả nếu tìm thấy
        if (conversation.isPresent()) {
            redisTemplate.opsForValue().set(cacheKey, conversation.get(), Duration.ofHours(1));
        }
        
        return conversation;
    }
    /**
     * Xóa cache DM cho conversation bị xóa
     */
    private void clearDmCache(UUID conversationId) {
        try {
            // Lấy danh sách members của conversation này
            List<ConversationMembers> members = memberRepository.findByKeyConversationId(conversationId);
            
            if (members.size() == 2) {
                List<UUID> userIds = members.stream()
                        .map(m -> m.getKey().getUserId())
                        .sorted()
                        .toList();
                
                String dmCacheKey = String.format("dmChat:%s:%s", userIds.get(0), userIds.get(1));
                redisTemplate.delete(dmCacheKey);
                
                logger.info("Cleared DM cache for conversation: {}", conversationId);
            }
        } catch (Exception e) {
            logger.warn("Error clearing DM cache for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    public Page<ConversationSearchDto> searchConversations(UUID userId, String name, String type, Pageable pageable) {
        if (conversationElasticsearchService != null) {
            // Use Elasticsearch for search
            Page<ConversationDocument> documents = conversationElasticsearchService.searchConversations(userId, name, type, pageable);
            
            return documents.map(doc -> ConversationSearchDto.builder()
                    .conversationId(doc.getConversationId())
                    .name(doc.getName())
                    .type(doc.getType())
                    .description(doc.getDescription())
                    .avatar(doc.getAvatar())
                    .createdAt(doc.getCreatedAt())
                    .lastMessage(doc.getLastMessage())
                    .createdBy(doc.getCreatedBy())
                    .memberCount(doc.getMemberCount())
                    .memberIds(doc.getMemberIds())
                    .build());
        } else {
            // Fallback to regular database search (no elasticsearch)
            logger.warn("Elasticsearch is not available, search functionality is limited");
            throw new UnsupportedOperationException("Search functionality requires Elasticsearch to be enabled");
        }
    }

    // Hook to index conversations when created/updated
    private void indexConversationToElasticsearch(Conversation conversation) {
        if (conversationElasticsearchService != null) {
            try {
                conversationElasticsearchService.indexConversation(conversation);
            } catch (Exception e) {
                logger.warn("Failed to index conversation to Elasticsearch: {}", conversation.getConversationId(), e);
            }
        }
    }

    /**
     * Lấy danh sách conversation với đầy đủ thông tin, bao gồm tên người còn lại cho DM
     */
    public List<ConversationResponseDto> getUserConversationsWithDetails(UUID userId) {
        List<Conversation> conversations = getUserConversations(userId);
        
        return conversations.stream()
                .map(conv -> buildConversationResponse(conv, userId))
                .toList();
    }
    
    /**
     * Build conversation response với đầy đủ thông tin
     */
    private ConversationResponseDto buildConversationResponse(Conversation conversation, UUID currentUserId) {
        ConversationResponseDto.ConversationResponseDtoBuilder builder = ConversationResponseDto.builder()
                .conversationId(conversation.getConversationId())
                .type(conversation.getType())
                .description(conversation.getDescription())
                .createdBy(conversation.getCreated_by())
                .backgroundUrl(conversation.getBackground_url())
                .createdAt(conversation.getCreated_at())
                .updatedAt(conversation.getUpdated_at())
                .isDeleted(conversation.is_deleted());
        
        if ("dm".equals(conversation.getType())) {
            // For DM: Find the other participant and use their name
            ConversationResponseDto.UserProfileDto otherParticipant = findOtherParticipant(conversation, currentUserId);
            String displayName = otherParticipant.getDisplayName() != null ? 
                    otherParticipant.getDisplayName() : otherParticipant.getUsername();
            
            builder.name(displayName)
                   .otherParticipant(otherParticipant)
                   .memberCount(2);
        } else {
            // For groups: Use conversation name
            builder.name(conversation.getName())
                   .memberCount(getMemberCount(conversation.getConversationId()));
        }

        return builder.build();
    }
    
    /**
     * Tìm participant còn lại trong DM conversation
     */
    private ConversationResponseDto.UserProfileDto findOtherParticipant(Conversation conversation, UUID currentUserId) {
        List<ConversationMembers> members = memberRepository.findByKeyConversationId(conversation.getConversationId());
        
        UUID otherUserId = members.stream()
                .map(member -> member.getKey().getUserId())
                .filter(id -> !id.equals(currentUserId))
                .findFirst()
                .orElse(null);
        
        if (otherUserId != null) {
            return userService.getUserProfile(otherUserId);
        }
        
        // Return default if no other participant found
        return ConversationResponseDto.UserProfileDto.builder()
                .userId(UUID.randomUUID())
                .username("Unknown User")
                .displayName("Unknown User")
                .email(null)
                .avatarUrl(null)
                .isOnline(false)
                .build();
    }
    
    /**
     * Đếm số member trong conversation
     */
    private Integer getMemberCount(UUID conversationId) {
        List<ConversationMembers> members = memberRepository.findByKeyConversationId(conversationId);
        return members.size();
    }
    
    /**
     * Tạo hoặc lấy DM conversation giữa 2 user
     * Nếu đã tồn tại và chưa bị xóa thì trả về conversation đó
     * Nếu chưa tồn tại thì tạo mới
     */
    public Conversation getOrCreateDMConversation(UUID userId1, UUID userId2) {
        // Kiểm tra DM conversation đã tồn tại chưa
        Optional<Conversation> existingDM = findPrivateConversationWithCache(userId1, userId2);
        
        if (existingDM.isPresent()) {
            return existingDM.get();
        }
        
        // Tạo conversation request cho DM
        ConversationRequest dmRequest = new ConversationRequest();
        dmRequest.setType("dm");
        dmRequest.setName("Direct Message"); // Tên mặc định cho DM
        dmRequest.setDescription("Private conversation between two users");
        dmRequest.setMemberIds(List.of(userId2)); // Chỉ thêm user thứ 2, user1 sẽ là creator
        
        return createConversation(dmRequest, userId1);
    }
    
    /**
     * Cập nhật thông tin conversation (tên, mô tả, avatar)
     * Chỉ owner và admin mới được cập nhật
     */
    public Conversation updateConversation(UUID conversationId, String name, String description, 
                                          String backgroundUrl, UUID requesterId) {
        Optional<Conversation> conversationOpt = conversationRepository.findByConversationId(conversationId);
        
        if (conversationOpt.isEmpty() || conversationOpt.get().is_deleted()) {
            throw new RuntimeException("Conversation không tồn tại");
        }
        
        Conversation conversation = conversationOpt.get();
        
        // Kiểm tra quyền (owner hoặc admin)
        ConversationMembers member = memberRepository.findById(
                new ConversationMembers.ConversationMemberKey(conversationId, requesterId)
        ).orElseThrow(() -> new RuntimeException("Bạn không phải là member của conversation này"));
        
        String role = member.getRole();
        if (!role.equals("owner") && !role.equals("admin")) {
            throw new RuntimeException("Chỉ chủ phòng và admin mới được cập nhật thông tin");
        }
        
        // Cập nhật thông tin
        if (name != null && !name.trim().isEmpty()) {
            conversation.setName(name);
        }
        if (description != null) {
            conversation.setDescription(description);
        }
        if (backgroundUrl != null) {
            conversation.setBackground_url(backgroundUrl);
        }
        
        conversation.setUpdated_at(Instant.now());
        conversationRepository.save(conversation);
        
        // Update cache nếu cần
        String cacheKey = "conversation:" + conversationId;
        redisTemplate.opsForValue().set(cacheKey, conversation, Duration.ofHours(1));
        
        // Re-index to Elasticsearch if available
        if (conversationElasticsearchService != null) {
            try {
                conversationElasticsearchService.indexConversation(conversation);
            } catch (Exception e) {
                logger.warn("Failed to update conversation in Elasticsearch: {}", conversationId, e);
            }
        }
        
        logger.info("Updated conversation {} by user {}", conversationId, requesterId);
        return conversation;
    }
    
    /**
     * Xóa vĩnh viễn conversation (chỉ owner)
     * Khác với soft delete, hành động này không thể hoàn tác
     */
    public boolean permanentDeleteConversation(UUID conversationId, UUID userId) {
        Optional<Conversation> conversationOpt = conversationRepository.findByConversationId(conversationId);
        
        if (conversationOpt.isEmpty()) {
            return false;
        }
        
        Conversation conversation = conversationOpt.get();
        
        // Kiểm tra quyền (chỉ owner)
        if (!conversation.getCreated_by().equals(userId)) {
            throw new RuntimeException("Chỉ chủ phòng mới có quyền xóa phòng vĩnh viễn");
        }
        
        // Không cho phép xóa DM conversation
        if ("dm".equals(conversation.getType())) {
            throw new RuntimeException("Không thể xóa phòng chat riêng");
        }
        
        // Xóa tất cả members
        List<ConversationMembers> members = memberRepository.findByKeyConversationId(conversationId);
        memberRepository.deleteAll(members);
        
        // Xóa conversation
        conversationRepository.delete(conversation);
        
        // Xóa cache
        String cacheKey = "conversation:" + conversationId;
        redisTemplate.delete(cacheKey);
        
        // Delete from Elasticsearch
        if (conversationElasticsearchService != null) {
            conversationElasticsearchService.deleteConversation(conversationId);
        }
        
        logger.info("Permanently deleted conversation {} by user {}", conversationId, userId);
        return true;
    }
}