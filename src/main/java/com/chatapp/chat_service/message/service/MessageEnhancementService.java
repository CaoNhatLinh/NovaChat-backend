package com.chatapp.chat_service.message.service;

import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.kafka.KafkaEventProducer;
import com.chatapp.chat_service.message.dto.MessageAttachmentDto;
import com.chatapp.chat_service.message.dto.MessageReactionDto;
import com.chatapp.chat_service.message.entity.Message;
import com.chatapp.chat_service.message.entity.MessageAttachment;
import com.chatapp.chat_service.message.entity.MessageReaction;
import com.chatapp.chat_service.message.entity.MessageReadReceipt;
import com.chatapp.chat_service.message.entity.PinnedMessage;
import com.chatapp.chat_service.message.event.MessageReactionEvent;
import com.chatapp.chat_service.message.event.MessageReadEvent;
import com.chatapp.chat_service.message.repository.MessageAttachmentRepository;
import com.chatapp.chat_service.message.repository.MessageReactionRepository;
import com.chatapp.chat_service.message.repository.MessageReadReceiptRepository;
import com.chatapp.chat_service.message.repository.MessageRepository;
import com.chatapp.chat_service.message.repository.PinnedMessageRepository;
import com.chatapp.chat_service.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageEnhancementService {

    private final MessageAttachmentRepository attachmentRepository;
    private final MessageReactionRepository reactionRepository;
    private final MessageReadReceiptRepository readReceiptRepository;
    private final PinnedMessageRepository pinnedMessageRepository;
    private final MessageRepository messageRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaEventProducer kafkaEventProducer;

    // ==================== ATTACHMENT METHODS ====================

    /**
     * Thêm attachment vào message
     */
    public MessageAttachmentDto addAttachment(UUID conversationId, UUID messageId, MessageAttachmentDto attachmentDto) {
        UUID attachmentId = UUID.randomUUID();
        
        MessageAttachment attachment = MessageAttachment.builder()
                .key(new MessageAttachment.MessageAttachmentKey(conversationId, messageId, attachmentId))
                .attachmentType(attachmentDto.getAttachmentType())
                .fileName(attachmentDto.getFileName())
                .url(attachmentDto.getUrl())
                .fileSize(attachmentDto.getFileSize())
                .mimeType(attachmentDto.getMimeType())
                .build();

        attachmentRepository.save(attachment);

        // Cache attachment
        String cacheKey = "message_attachments:" + conversationId + ":" + messageId;
        redisTemplate.opsForList().rightPush(cacheKey, attachment);
        redisTemplate.expire(cacheKey, Duration.ofHours(1));

        log.info("Added attachment {} to message {} in conversation {}", attachmentId, messageId, conversationId);

        return MessageAttachmentDto.builder()
                .attachmentId(attachmentId)
                .attachmentType(attachment.getAttachmentType())
                .fileName(attachment.getFileName())
                .url(attachment.getUrl())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .build();
    }

    /**
     * Lấy attachments của message
     */
    public List<MessageAttachmentDto> getMessageAttachments(UUID conversationId, UUID messageId) {
        String cacheKey = "message_attachments:" + conversationId + ":" + messageId;
        
        // Try cache first
        List<Object> cachedAttachments = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (cachedAttachments != null && !cachedAttachments.isEmpty()) {
            return cachedAttachments.stream()
                    .map(obj -> (MessageAttachment) obj)
                    .map(this::mapToAttachmentDto)
                    .collect(Collectors.toList());
        }

        // Fallback to database
        List<MessageAttachment> attachments = attachmentRepository.findByConversationIdAndMessageId(conversationId, messageId);
        
        // Cache result
        if (!attachments.isEmpty()) {
            attachments.forEach(attachment -> 
                redisTemplate.opsForList().rightPush(cacheKey, attachment));
            redisTemplate.expire(cacheKey, Duration.ofHours(1));
        }

        return attachments.stream()
                .map(this::mapToAttachmentDto)
                .collect(Collectors.toList());
    }

    // ==================== REACTION METHODS ====================

    /**
     * Thêm hoặc xóa reaction
     */
    public void toggleReaction(UUID conversationId, UUID messageId, String emoji, UUID userId) {
        MessageReaction.MessageReactionKey key = new MessageReaction.MessageReactionKey(conversationId, messageId, emoji, userId);
        
        Optional<MessageReaction> existingReaction = reactionRepository.findById(key);
        boolean isRemoving = existingReaction.isPresent();
        
        if (isRemoving) {
            // Remove reaction
            reactionRepository.delete(existingReaction.get());
            log.info("Removed reaction {} from user {} on message {}", emoji, userId, messageId);
        } else {
            // Add reaction
            MessageReaction reaction = MessageReaction.builder()
                    .key(key)
                    .reactedAt(Instant.now())
                    .build();
            reactionRepository.save(reaction);
            log.info("Added reaction {} from user {} on message {}", emoji, userId, messageId);
        }

        // Clear cache
        clearReactionCache(conversationId, messageId);

        // Send real-time update
        MessageReactionEvent event = MessageReactionEvent.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .emoji(emoji)
                .userId(userId)
                .action(isRemoving ? "REMOVE" : "ADD")
                .timestamp(Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/reactions", event);

        // Send to Kafka for further processing
        kafkaEventProducer.sendReactionEvent(event);

        // Create notification for message owner if it's not their own reaction and it's an ADD action
        if (!isRemoving && !userId.equals(getMessageOwnerId(conversationId, messageId))) {
            try {
                UUID messageOwnerId = getMessageOwnerId(conversationId, messageId);
                String userName = getUserName(userId); // You might need to implement this
                notificationService.createReactionNotification(messageOwnerId, userId, userName, emoji, conversationId, messageId);
            } catch (Exception e) {
                log.warn("Failed to create reaction notification for message {}: {}", messageId, e.getMessage());
            }
        }
    }

    /**
     * Get message owner ID
     */
    private UUID getMessageOwnerId(UUID conversationId, UUID messageId) {
        Message.MessageKey key = new Message.MessageKey(conversationId, messageId);
        Optional<Message> message = messageRepository.findById(key);
        return message.map(Message::getSenderId).orElse(null);
    }

    /**
     * Get user name (simplified implementation)
     */
    private String getUserName(UUID userId) {
        // In real implementation, you would get this from UserService
        // For now, return a placeholder
        return "User-" + userId.toString().substring(0, 8);
    }

    /**
     * Lấy reactions của message
     */
    public List<MessageReactionDto> getMessageReactions(UUID conversationId, UUID messageId, UUID currentUserId) {
        String cacheKey = "message_reactions:" + conversationId + ":" + messageId;
        
        // Try cache first
        Map<Object, Object> cachedReactions = redisTemplate.opsForHash().entries(cacheKey);
        if (!cachedReactions.isEmpty()) {
            return buildReactionDtos(cachedReactions, currentUserId);
        }

        // Fallback to database
        List<MessageReaction> reactions = reactionRepository.findByConversationIdAndMessageId(conversationId, messageId);
        
        // Group by emoji
        Map<String, List<MessageReaction>> groupedReactions = reactions.stream()
                .collect(Collectors.groupingBy(r -> r.getKey().getEmoji()));

        // Cache grouped reactions
        groupedReactions.forEach((emoji, reactionList) -> {
            Map<String, Object> emojiData = new HashMap<>();
            emojiData.put("userIds", reactionList.stream().map(r -> r.getKey().getUserId()).collect(Collectors.toList()));
            emojiData.put("count", reactionList.size());
            emojiData.put("lastReactedAt", reactionList.stream().map(MessageReaction::getReactedAt).max(Instant::compareTo).orElse(Instant.now()));
            
            redisTemplate.opsForHash().put(cacheKey, emoji, emojiData);
        });
        redisTemplate.expire(cacheKey, Duration.ofMinutes(30));

        return groupedReactions.entrySet().stream()
                .map(entry -> {
                    String emoji = entry.getKey();
                    List<MessageReaction> reactionList = entry.getValue();
                    List<UUID> userIds = reactionList.stream().map(r -> r.getKey().getUserId()).collect(Collectors.toList());
                    
                    return MessageReactionDto.builder()
                            .messageId(messageId)
                            .emoji(emoji)
                            .createdAt(reactionList.stream().map(MessageReaction::getReactedAt)
                                .max(Instant::compareTo).orElse(Instant.now())
                                .atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .user(UserDTO.builder()
                                .user_id(reactionList.get(0).getKey().getUserId())
                                .username("user_" + reactionList.get(0).getKey().getUserId().toString().substring(0, 8))
                                .display_name("User " + reactionList.get(0).getKey().getUserId().toString().substring(0, 8))
                                .build())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== READ RECEIPT METHODS ====================

    /**
     * Đánh dấu message đã đọc
     */
    public void markAsRead(UUID conversationId, UUID messageId, UUID readerId) {
        MessageReadReceipt.MessageReadReceiptKey key = new MessageReadReceipt.MessageReadReceiptKey(conversationId, messageId, readerId);
        
        // Check if already read
        Optional<MessageReadReceipt> existing = readReceiptRepository.findById(key);
        if (existing.isPresent()) {
            return; // Already marked as read
        }

        MessageReadReceipt receipt = MessageReadReceipt.builder()
                .key(key)
                .readAt(Instant.now())
                .build();

        readReceiptRepository.save(receipt);

        // Clear cache
        String cacheKey = "message_read_receipts:" + conversationId + ":" + messageId;
        redisTemplate.delete(cacheKey);

        // Send real-time update
        MessageReadEvent event = MessageReadEvent.builder()
                .conversationId(conversationId)
                .messageId(messageId)
                .readerId(readerId)
                .readAt(Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/read", event);

        log.info("User {} marked message {} as read in conversation {}", readerId, messageId, conversationId);
    }

    /**
     * Lấy read receipts cho message
     */
    public List<MessageReadReceipt> getMessageReadReceipts(UUID conversationId, UUID messageId) {
        String cacheKey = "message_read_receipts:" + conversationId + ":" + messageId;
        
        // Try cache first
        List<Object> cachedReceipts = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (cachedReceipts != null && !cachedReceipts.isEmpty()) {
            return cachedReceipts.stream()
                    .map(obj -> (MessageReadReceipt) obj)
                    .collect(Collectors.toList());
        }

        // Fallback to database
        List<MessageReadReceipt> receipts = readReceiptRepository.findByConversationIdAndMessageId(conversationId, messageId);
        
        // Cache result
        if (!receipts.isEmpty()) {
            receipts.forEach(receipt -> 
                redisTemplate.opsForList().rightPush(cacheKey, receipt));
            redisTemplate.expire(cacheKey, Duration.ofMinutes(15));
        }

        return receipts;
    }

    // ==================== PINNED MESSAGE METHODS ====================

    /**
     * Pin/Unpin message
     */
    public void togglePinMessage(UUID conversationId, UUID messageId, UUID pinnedBy) {
        PinnedMessage.PinnedMessageKey key = new PinnedMessage.PinnedMessageKey(conversationId, messageId);
        
        Optional<PinnedMessage> existing = pinnedMessageRepository.findById(key);
        
        if (existing.isPresent()) {
            // Unpin
            pinnedMessageRepository.delete(existing.get());
            log.info("Unpinned message {} in conversation {} by user {}", messageId, conversationId, pinnedBy);
        } else {
            // Pin
            PinnedMessage pinnedMessage = PinnedMessage.builder()
                    .key(key)
                    .pinnedAt(Instant.now())
                    .pinnedBy(pinnedBy)
                    .build();
            pinnedMessageRepository.save(pinnedMessage);
            log.info("Pinned message {} in conversation {} by user {}", messageId, conversationId, pinnedBy);
        }

        // Clear cache
        String cacheKey = "pinned_messages:" + conversationId;
        redisTemplate.delete(cacheKey);

        // Send real-time update
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/pins", 
            Map.of("messageId", messageId, "action", existing.isPresent() ? "UNPIN" : "PIN", "pinnedBy", pinnedBy));
    }

    /**
     * Lấy pinned messages
     */
    public List<PinnedMessage> getPinnedMessages(UUID conversationId) {
        String cacheKey = "pinned_messages:" + conversationId;
        
        // Try cache first
        List<Object> cachedPinned = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (cachedPinned != null && !cachedPinned.isEmpty()) {
            return cachedPinned.stream()
                    .map(obj -> (PinnedMessage) obj)
                    .collect(Collectors.toList());
        }

        // Fallback to database
        List<PinnedMessage> pinnedMessages = pinnedMessageRepository.findByConversationId(conversationId);
        
        // Cache result
        if (!pinnedMessages.isEmpty()) {
            pinnedMessages.forEach(pinned -> 
                redisTemplate.opsForList().rightPush(cacheKey, pinned));
            redisTemplate.expire(cacheKey, Duration.ofMinutes(30));
        }

        return pinnedMessages;
    }

    // ==================== HELPER METHODS ====================

    private MessageAttachmentDto mapToAttachmentDto(MessageAttachment attachment) {
        return MessageAttachmentDto.builder()
                .attachmentId(attachment.getKey().getAttachmentId())
                .attachmentType(attachment.getAttachmentType())
                .fileName(attachment.getFileName())
                .url(attachment.getUrl())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .build();
    }

    private List<MessageReactionDto> buildReactionDtos(Map<Object, Object> cachedReactions, UUID currentUserId) {
        return cachedReactions.entrySet().stream()
                .map(entry -> {
                    String emoji = (String) entry.getKey();
                    Map<String, Object> data = (Map<String, Object>) entry.getValue();
                    List<UUID> userIds = (List<UUID>) data.get("userIds");
                    
                    return MessageReactionDto.builder()
                            .messageId(UUID.randomUUID()) // TODO: Get actual message ID
                            .emoji(emoji)
                            .createdAt(((Instant) data.get("lastReactedAt"))
                                .atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .user(UserDTO.builder()
                                .user_id(userIds.get(0))
                                .username("user_" + userIds.get(0).toString().substring(0, 8))
                                .display_name("User " + userIds.get(0).toString().substring(0, 8))
                                .build())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void clearReactionCache(UUID conversationId, UUID messageId) {
        String cacheKey = "message_reactions:" + conversationId + ":" + messageId;
        redisTemplate.delete(cacheKey);
    }
}
