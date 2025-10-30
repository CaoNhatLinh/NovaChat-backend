package com.chatapp.chat_service.message.service;

import com.chatapp.chat_service.elasticsearch.service.ConversationElasticsearchService;
import com.chatapp.chat_service.message.dto.MessageRequest;
import com.chatapp.chat_service.message.dto.MessageResponseDto;
import com.chatapp.chat_service.message.dto.MessageSummary;
import com.chatapp.chat_service.message.entity.Message;
import com.chatapp.chat_service.message.mapper.MessageMapper;
import com.chatapp.chat_service.message.repository.MessageRepository;
import com.chatapp.chat_service.security.core.SecurityContextHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datastax.oss.driver.api.core.uuid.Uuids;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final SecurityContextHelper securityContextHelper;
    private final ConversationElasticsearchService conversationElasticsearchService;
    private final MessageMapper messageMapper;
    private final MessageValidationService messageValidationService;

    public MessageService(MessageRepository messageRepository,
                         SecurityContextHelper securityContextHelper,
                         MessageMapper messageMapper,
                         MessageValidationService messageValidationService,
                         @Autowired(required = false) ConversationElasticsearchService conversationElasticsearchService) {
        this.messageRepository = messageRepository;
        this.securityContextHelper = securityContextHelper;
        this.messageMapper = messageMapper;
        this.messageValidationService = messageValidationService;
        this.conversationElasticsearchService = conversationElasticsearchService;
    }

    @Transactional
    public MessageResponseDto sendMessage(MessageRequest request) {
        // Use senderId from request if provided, otherwise fallback to security context
        UUID senderId = request.getSenderId() != null ? 
                       request.getSenderId() : 
                       securityContextHelper.getCurrentUserId();
                       
        System.out.println("MessageService.sendMessage - Using senderId: " + senderId);
        messageValidationService.validateConversationMembership(request.getConversationId(), senderId);

        Message.MessageKey key = new Message.MessageKey(
                request.getConversationId(),
                Uuids.timeBased()
        );

        Message message = Message.builder()
                .key(key)
                .senderId(senderId)
                .content(request.getContent())
                .createdAt(Instant.now())
                .isDeleted(false)
                .type(request.getType())
                .mentionedUserIds(request.getMentionedUserIds())
                .replyTo(request.getReplyTo())
                .build();

        Message savedMessage = messageRepository.save(message);


        // Update last message in Elasticsearch if available
        if (conversationElasticsearchService != null) {
            MessageSummary messageSummary = MessageSummary.builder()
                    .messageId(savedMessage.getKey().getMessageId())
                    .senderId(savedMessage.getSenderId())
                    .content(savedMessage.getContent())
                    .createdAt(savedMessage.getCreatedAt())
                    .build();
            conversationElasticsearchService.updateLastMessage(request.getConversationId(), messageSummary);
        }

        return messageMapper.toResponseDto(savedMessage);
    }

    /**
     * Lấy tin nhắn mới nhất của conversation (mặc định 20 tin)
     * @param conversationId ID của conversation
     * @param pageable Pageable với default size = 20
     * @return List MessageResponseDto được sắp xếp ASC (cũ nhất trước, mới nhất sau)
     * 
     * Note: Cassandra trả về messages theo thứ tự DESC (mới nhất trước) do CLUSTERING ORDER BY (message_id DESC),
     * nhưng frontend cần thứ tự ASC (cũ nhất trước) để hiển thị đúng trong chat UI.
     */
    public List<MessageResponseDto> getLatestMessages(UUID conversationId, Pageable pageable) {
        UUID userId = securityContextHelper.getCurrentUserId();
        messageValidationService.validateConversationMembership(conversationId, userId);

        int limit = pageable.getPageSize();
        System.out.println("🔍 [getLatestMessages] DEBUG INFO:");
        System.out.println("   - Conversation ID: " + conversationId);
        System.out.println("   - Requested limit: " + limit);
        System.out.println("   - Pageable details: " + pageable);
        System.out.println("   - Method: Custom @Query with LIMIT");

        // Sử dụng query với LIMIT trực tiếp để đảm bảo chỉ lấy đúng số lượng
        List<Message> messages = messageRepository.findByConversationIdWithLimit(conversationId, limit);
        
        System.out.println("   - Messages found: " + messages.size() + " (should be <= " + limit + ")");
        if (messages.size() > limit) {
            System.err.println("   ⚠️  WARNING: Found more messages than limit! This should not happen!");
        }
        
        // 🔄 Đảo ngược danh sách messages (từ DESC sang ASC - cũ nhất trước)
        List<MessageResponseDto> responseList = messages.stream()
                .map(messageMapper::toResponseDto)
                .collect(Collectors.toList());
        
        // Reverse the list để có thứ tự cũ nhất trước (ASC order)
        Collections.reverse(responseList);
        System.out.println("   - List reversed: Oldest messages first");
        
        return responseList;
    }

    /**
     * Overloaded method với default PageRequest size = 20
     */
    public List<MessageResponseDto> getLatestMessages(UUID conversationId) {
        return getLatestMessages(conversationId, PageRequest.of(0, 20));
    }

    /**
     * Lấy tin nhắn cũ hơn một message nhất định (pagination thủ công)
     * Sử dụng TIMEUUID để query hiệu quả
     * @param conversationId ID của conversation
     * @param beforeMessageId Message ID làm điểm reference (lấy những tin nhắn cũ hơn message này)
     * @return List của 30 MessageResponseDto cũ hơn beforeMessageId, sắp xếp ASC (cũ nhất trước)
     * 
     * Note: Messages được reverse để có thứ tự ASC phù hợp với frontend chat UI
     */
    public List<MessageResponseDto> getOlderMessages(UUID conversationId, UUID beforeMessageId) {
        UUID userId = securityContextHelper.getCurrentUserId();
        messageValidationService.validateConversationMembership(conversationId, userId);

        System.out.println("Getting older messages for conversation: " + conversationId + ", before messageId: " + beforeMessageId);

        // Sử dụng findOlderMessages để lấy 30 tin nhắn cũ hơn beforeMessageId
        List<Message> olderMessages = messageRepository.findOlderMessages(conversationId, beforeMessageId);
        
        System.out.println("Found " + olderMessages.size() + " older messages");

        // 🔄 Đảo ngược danh sách older messages (từ DESC sang ASC - cũ nhất trước)
        List<MessageResponseDto> responseList = olderMessages.stream()
                .map(messageMapper::toResponseDto)
                .collect(Collectors.toList());
        
        // Reverse the list để có thứ tự cũ nhất trước (ASC order)
        Collections.reverse(responseList);
        System.out.println("Older messages list reversed: Oldest messages first");
        
        return responseList;
    }

    /**
     * @deprecated Sử dụng getLatestMessages() thay thế
     */
    @Deprecated
    public List<MessageResponseDto> getMessages(UUID conversationId, Pageable pageable) {
        return getLatestMessages(conversationId, pageable);
    }

    /**
     * Lấy tin nhắn của conversation với các filter thời gian
     * @param conversationId ID của conversation
     * @param before Lấy tin nhắn trước thời điểm này
     * @param after Lấy tin nhắn sau thời điểm này
     * @param pageable Pageable (mặc định size = 20)
     * @return List MessageResponseDto
     */
    public List<MessageResponseDto> getConversationMessages(UUID conversationId, LocalDateTime before, LocalDateTime after, Pageable pageable) {
        UUID userId = securityContextHelper.getCurrentUserId();
        messageValidationService.validateConversationMembership(conversationId, userId);

        // Đảm bảo pageable có size hợp lý
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100);
        }

        List<Message> messages;

        if (before != null && after != null) {
            Instant beforeInstant = before.atZone(ZoneId.systemDefault()).toInstant();
            Instant afterInstant = after.atZone(ZoneId.systemDefault()).toInstant();
            messages = messageRepository.findByConversationIdAndTimestampBetween(conversationId, afterInstant, beforeInstant, pageable);
        } else if (before != null) {
            Instant beforeInstant = before.atZone(ZoneId.systemDefault()).toInstant();
            messages = messageRepository.findByConversationIdAndTimestampBefore(conversationId, beforeInstant, pageable);
        } else if (after != null) {
            Instant afterInstant = after.atZone(ZoneId.systemDefault()).toInstant();
            messages = messageRepository.findByConversationIdAndTimestampAfter(conversationId, afterInstant, pageable);
        } else {
            // Nếu không có filter thời gian, sử dụng getLatestMessages
            return getLatestMessages(conversationId, pageable);
        }

        List<MessageResponseDto> responseList = messages.stream()
                .map(messageMapper::toResponseDto)
                .collect(Collectors.toList());
        
        // 🔄 Đảo ngược danh sách để có thứ tự cũ nhất trước (ASC order)
        Collections.reverse(responseList);
        
        return responseList;
    }

    /**
     * Overloaded method với default PageRequest size = 20
     */
    public List<MessageResponseDto> getConversationMessages(UUID conversationId, LocalDateTime before, LocalDateTime after) {
        return getConversationMessages(conversationId, before, after, PageRequest.of(0, 20));
    }

    /**
     * Alternative method sử dụng derived query (backup solution)
     * Sử dụng khi custom @Query không hoạt động đúng với Pageable
     */
    public List<MessageResponseDto> getLatestMessagesAlternative(UUID conversationId, Pageable pageable) {
        UUID userId = securityContextHelper.getCurrentUserId();
        messageValidationService.validateConversationMembership(conversationId, userId);

        System.out.println("Getting latest messages (alternative) for conversation: " + conversationId + ", page size: " + pageable.getPageSize());

        // Sử dụng derived query method - Spring Data tự động hỗ trợ Pageable
        List<Message> messages = messageRepository.findByKeyConversationIdOrderByKeyMessageIdDesc(conversationId, pageable);
        
        System.out.println("Found " + messages.size() + " latest messages (alternative method)");
        
        // 🔄 Đảo ngược danh sách messages (từ DESC sang ASC - cũ nhất trước)
        List<MessageResponseDto> responseList = messages.stream()
                .map(messageMapper::toResponseDto)
                .collect(Collectors.toList());
        
        // Reverse the list để có thứ tự cũ nhất trước (ASC order)
        Collections.reverse(responseList);
        System.out.println("Alternative method list reversed: Oldest messages first");
        
        return responseList;
    }

}