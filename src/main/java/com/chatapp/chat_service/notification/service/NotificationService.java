package com.chatapp.chat_service.notification.service;

import com.chatapp.chat_service.kafka.KafkaEventProducer;
import com.chatapp.chat_service.notification.dto.ConversationNotificationDto;
import com.chatapp.chat_service.notification.dto.NotificationDto;
import com.chatapp.chat_service.notification.dto.NotificationStatsDto;
import com.chatapp.chat_service.notification.entity.Notification;
import com.chatapp.chat_service.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Tạo notification mới
     */
    public NotificationDto createNotification(UUID userId, String title, String body, String type, Map<String, Object> metadata) {
        UUID notificationId = UUID.randomUUID();
        
        Notification notification = Notification.builder()
                .userId(userId)
                .notificationId(notificationId)
                .title(title)
                .body(body)
                .type(type)
                .metadata(serializeMetadata(metadata))
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        // Clear cache
        clearUserNotificationCache(userId);

        NotificationDto dto = mapToDto(notification);

        // Send real-time notification
        sendRealtimeNotification(userId, dto);

        log.info("Created notification {} for user {}", notificationId, userId);
        return dto;
    }

    /**
     * Tạo notification cho tin nhắn mới
     */
    public void createMessageNotification(UUID recipientId, UUID conversationId, UUID messageId, 
                                        String senderName, String messageContent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversationId", conversationId.toString());
        metadata.put("messageId", messageId.toString());
        metadata.put("senderName", senderName);

        String title = "Tin nhắn mới từ " + senderName;
        String body = messageContent.length() > 100 ? messageContent.substring(0, 100) + "..." : messageContent;

        createNotification(recipientId, title, body, Notification.NotificationType.MESSAGE, metadata);

        // Update conversation notification
        updateConversationNotification(recipientId, conversationId, messageId, messageContent, senderName);
    }

    /**
     * Tạo notification cho reaction mới
     */
    public void createReactionNotification(UUID recipientId, UUID reactorId, String reactorName, 
                                         String emoji, UUID conversationId, UUID messageId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversationId", conversationId.toString());
        metadata.put("messageId", messageId.toString());
        metadata.put("reactorId", reactorId.toString());
        metadata.put("reactorName", reactorName);
        metadata.put("emoji", emoji);

        String title = "Reaction mới từ " + reactorName;
        String body = reactorName + " đã react " + emoji + " với tin nhắn của bạn";

        createNotification(recipientId, title, body, Notification.NotificationType.REACTION, metadata);
    }

    /**
     * Tạo notification cho mention
     */
    public void createMentionNotification(UUID recipientId, UUID mentionerId, String mentionerName,
                                        UUID conversationId, UUID messageId, String messageContent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversationId", conversationId.toString());
        metadata.put("messageId", messageId.toString());
        metadata.put("mentionerId", mentionerId.toString());
        metadata.put("mentionerName", mentionerName);

        String title = "Bạn được mention bởi " + mentionerName;
        String body = messageContent.length() > 100 ? messageContent.substring(0, 100) + "..." : messageContent;

        createNotification(recipientId, title, body, Notification.NotificationType.MENTION, metadata);
    }

    /**
     * Tạo notification cho friend request
     */
    public void createFriendRequestNotification(UUID recipientId, UUID requesterId, String requesterName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("requesterId", requesterId.toString());
        metadata.put("requesterName", requesterName);

        String title = "Lời mời kết bạn";
        String body = requesterName + " đã gửi lời mời kết bạn cho bạn";

        createNotification(recipientId, title, body, Notification.NotificationType.FRIEND_REQUEST, metadata);
    }

    /**
     * Tạo notification cho conversation invite
     */
    public void createConversationInviteNotification(UUID recipientId, UUID inviterId, String inviterName, 
                                                   UUID conversationId, String conversationName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("inviterId", inviterId.toString());
        metadata.put("inviterName", inviterName);
        metadata.put("conversationId", conversationId.toString());
        metadata.put("conversationName", conversationName);

        String title = "Mời tham gia cuộc trò chuyện";
        String body = inviterName + " đã mời bạn tham gia \"" + conversationName + "\"";

        createNotification(recipientId, title, body, Notification.NotificationType.CONVERSATION_INVITE, metadata);
    }

    /**
     * Tạo notification cho friend request update
     */
    public void sendFriendshipUpdateNotification(UUID recipientId, UUID senderId, String status) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("senderId", senderId.toString());
        metadata.put("status", status);

        String title;
        String body;
        String notificationType;

        switch (status.toUpperCase()) {
            case "ACCEPTED":
                title = "Lời mời kết bạn được chấp nhận";
                body = "Lời mời kết bạn của bạn đã được chấp nhận";
                notificationType = Notification.NotificationType.FRIEND_REQUEST;
                break;
            case "REJECTED":
                title = "Lời mời kết bạn bị từ chối";
                body = "Lời mời kết bạn của bạn đã bị từ chối";
                notificationType = Notification.NotificationType.FRIEND_REQUEST;
                break;
            case "PENDING":
                title = "Lời mời kết bạn mới";
                body = "Bạn có một lời mời kết bạn mới";
                notificationType = Notification.NotificationType.FRIEND_REQUEST;
                break;
            default:
                title = "Cập nhật tình bạn";
                body = "Có cập nhật về tình bạn của bạn";
                notificationType = Notification.NotificationType.SYSTEM;
                break;
        }

        createNotification(recipientId, title, body, notificationType, metadata);
    }

    /**
     * Tạo notification cho poll
     */
    public void createPollNotification(UUID recipientId, UUID creatorId, String creatorName,
                                     UUID conversationId, UUID pollId, String pollQuestion) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversationId", conversationId.toString());
        metadata.put("pollId", pollId.toString());
        metadata.put("creatorId", creatorId.toString());
        metadata.put("creatorName", creatorName);

        String title = "Poll mới từ " + creatorName;
        String body = "\"" + pollQuestion + "\"";

        createNotification(recipientId, title, body, Notification.NotificationType.POLL, metadata);
    }

    /**
     * Tạo notification cho pinned message
     */
    public void createPinMessageNotification(UUID recipientId, UUID pinnerId, String pinnerName,
                                           UUID conversationId, UUID messageId, String messageContent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversationId", conversationId.toString());
        metadata.put("messageId", messageId.toString());
        metadata.put("pinnerId", pinnerId.toString());
        metadata.put("pinnerName", pinnerName);

        String title = "Tin nhắn được ghim";
        String body = pinnerName + " đã ghim một tin nhắn: " + 
                     (messageContent.length() > 50 ? messageContent.substring(0, 50) + "..." : messageContent);

        createNotification(recipientId, title, body, Notification.NotificationType.PIN_MESSAGE, metadata);
    }

    /**
     * Tạo notification cho system announcement
     */
    public void createSystemNotification(UUID recipientId, String title, String body, Map<String, Object> metadata) {
        createNotification(recipientId, title, body, Notification.NotificationType.SYSTEM, metadata);
    }

    // Removed duplicate method - using enhanced version below

    /**
     * Lấy notifications theo type với phân trang
     */
    public NotificationPage getNotificationsByType(UUID userId, String type, int page, int size) {
        String cacheKey = "user_notifications_type:" + userId + ":" + type + ":" + page + ":" + size;
        
        // Try cache first
        @SuppressWarnings("unchecked")
        List<NotificationDto> cachedNotifications = (List<NotificationDto>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedNotifications != null) {
            boolean hasNext = cachedNotifications.size() == size; // Simplified check
            return new NotificationPage(cachedNotifications, hasNext, !cachedNotifications.isEmpty());
        }

        // Query database
        Pageable pageable = PageRequest.of(page, size);
        Slice<Notification> notifications = notificationRepository.findByUserIdAndType(userId, type, pageable);
        
        List<NotificationDto> notificationDtos = notifications.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // Cache result
        redisTemplate.opsForValue().set(cacheKey, notificationDtos, Duration.ofMinutes(10));

        return new NotificationPage(notificationDtos, notifications.hasNext(), notifications.hasContent());
    }

    /**
     * Lấy notifications với phân trang (wrapper method)
     */
    public NotificationPage getNotifications(UUID userId, Pageable pageable) {
        return getNotifications(userId, pageable.getPageNumber(), pageable.getPageSize());
    }

    /**
     * Lấy notifications với phân trang
     */
    public NotificationPage getNotifications(UUID userId, int page, int size) {
        String cacheKey = "user_notifications:" + userId + ":" + page + ":" + size;
        
        // Try cache first
        @SuppressWarnings("unchecked")
        List<NotificationDto> cachedNotifications = (List<NotificationDto>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedNotifications != null) {
            boolean hasNext = cachedNotifications.size() == size;
            return new NotificationPage(cachedNotifications, hasNext, !cachedNotifications.isEmpty());
        }

        // Query database
        Pageable pageable = PageRequest.of(page, size);
        Slice<Notification> notifications = notificationRepository.findByUserId(userId, pageable);
        
        List<NotificationDto> notificationDtos = notifications.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // Cache result
        redisTemplate.opsForValue().set(cacheKey, notificationDtos, Duration.ofMinutes(10));

        return new NotificationPage(notificationDtos, notifications.hasNext(), notifications.hasContent());
    }

    /**
     * Lấy unread count với cache
     */
    public Long getUnreadCount(UUID userId) {
        String cacheKey = "unread_count:" + userId;
        
        // Try cache first
        Long cachedCount = (Long) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCount != null) {
            return cachedCount;
        }

        // Query database
        long count = notificationRepository.countUnreadByUserId(userId);
        
        // Cache result
        redisTemplate.opsForValue().set(cacheKey, count, Duration.ofMinutes(5));
        
        return count;
    }

    /**
     * Đánh dấu notification đã đọc
     */
    public void markAsRead(UUID userId, UUID notificationId) {
        notificationRepository.markAsRead(userId, notificationId);
        clearUserNotificationCache(userId);
        
        // Send real-time update
        Map<String, Object> update = new HashMap<>();
        update.put("notificationId", notificationId);
        update.put("isRead", true);
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notification-read", update);

        log.info("Marked notification {} as read for user {}", notificationId, userId);
    }

    /**
     * Đánh dấu tất cả notifications đã đọc
     */
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
        clearUserNotificationCache(userId);
        
        // Send real-time update
        Map<String, Object> update = new HashMap<>();
        update.put("action", "MARK_ALL_READ");
        update.put("timestamp", Instant.now());
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notification-read", update);

        log.info("Marked all notifications as read for user {}", userId);
    }

    /**
     * Xóa notification
     */
    public void deleteNotification(UUID userId, UUID notificationId) {
        notificationRepository.deleteByUserIdAndNotificationId(userId, notificationId);
        clearUserNotificationCache(userId);
        
        // Send real-time update
        Map<String, Object> update = new HashMap<>();
        update.put("notificationId", notificationId);
        update.put("action", "DELETE");
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notification-delete", update);

        log.info("Deleted notification {} for user {}", notificationId, userId);
    }

    /**
     * Xóa tất cả notifications của user
     */
    public void deleteAllNotifications(UUID userId) {
        notificationRepository.deleteByUserId(userId);
        clearUserNotificationCache(userId);
        
        // Send real-time update
        Map<String, Object> update = new HashMap<>();
        update.put("action", "DELETE_ALL");
        update.put("timestamp", Instant.now());
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notification-delete", update);

        log.info("Deleted all notifications for user {}", userId);
    }

    /**
     * Lấy notification statistics
     */
    public NotificationStatsDto getNotificationStats(UUID userId) {
        String cacheKey = "notification_stats:" + userId;
        
        // Try cache first
        NotificationStatsDto cachedStats = (NotificationStatsDto) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStats != null) {
            return cachedStats;
        }

        // Query database
        long totalCount = notificationRepository.countByUserId(userId);
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        long readCount = totalCount - unreadCount;
        
        // Get notifications from last 7 days
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long weeklyCount = notificationRepository.countByUserIdAndCreatedAtAfter(userId, weekAgo);
        
        // Get notifications by type
        Map<String, Long> typeStats = new HashMap<>();
        for (String type : List.of("MESSAGE", "REACTION", "MENTION", "FRIEND_REQUEST", "CONVERSATION_INVITE", "POLL", "PIN_MESSAGE", "SYSTEM")) {
            long count = notificationRepository.countByUserIdAndType(userId, type);
            if (count > 0) {
                typeStats.put(type, count);
            }
        }

        NotificationStatsDto stats = NotificationStatsDto.builder()
                .userId(userId)
                .totalCount(totalCount)
                .unreadCount(unreadCount)
                .readCount(readCount)
                .weeklyCount(weeklyCount)
                .typeStats(typeStats)
                .lastUpdated(Instant.now())
                .build();

        // Cache result
        redisTemplate.opsForValue().set(cacheKey, stats, Duration.ofHours(1));
        
        return stats;
    }

    /**
     * Bulk mark notifications as read
     */
    public void bulkMarkAsRead(UUID userId, List<UUID> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }

        notificationRepository.bulkMarkAsRead(userId, notificationIds);
        clearUserNotificationCache(userId);
        
        // Send real-time update
        Map<String, Object> update = new HashMap<>();
        update.put("notificationIds", notificationIds);
        update.put("action", "BULK_MARK_READ");
        update.put("timestamp", Instant.now());
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notification-read", update);

        log.info("Bulk marked {} notifications as read for user {}", notificationIds.size(), userId);
    }

    /**
     * Get notifications by date range
     */
    public List<NotificationDto> getNotificationsByDateRange(UUID userId, Instant startDate, Instant endDate) {
        List<Notification> notifications = notificationRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate);
        return notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Search notifications by content
     */
    public List<NotificationDto> searchNotifications(UUID userId, String searchTerm, int limit) {
        List<Notification> notifications = notificationRepository.searchByUserIdAndContent(userId, searchTerm, limit);
        return notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has unread notifications
     */
    public boolean hasUnreadNotifications(UUID userId) {
        return getUnreadCount(userId) > 0;
    }

    /**
     * Get latest notification for user
     */
    public Optional<NotificationDto> getLatestNotification(UUID userId) {
        Optional<Notification> latest = notificationRepository.findLatestByUserId(userId);
        return latest.map(this::mapToDto);
    }

    /**
     * Lấy tất cả notifications chưa đọc
     */
    public List<NotificationDto> getUnreadNotifications(UUID userId) {
        String cacheKey = "unread_notifications:" + userId;
        
        // Try cache first
        @SuppressWarnings("unchecked")
        List<NotificationDto> cachedNotifications = (List<NotificationDto>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedNotifications != null) {
            return cachedNotifications;
        }

        // Query database
        List<Notification> notifications = notificationRepository.findUnreadByUserId(userId);
        
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // Cache result
        redisTemplate.opsForValue().set(cacheKey, notificationDtos, Duration.ofMinutes(5));

        return notificationDtos;
    }

    /**
     * Notification Page wrapper class
     */
    public static class NotificationPage {
        private final List<NotificationDto> content;
        private final boolean hasNext;
        private final boolean hasContent;

        public NotificationPage(List<NotificationDto> content, boolean hasNext, boolean hasContent) {
            this.content = content;
            this.hasNext = hasNext;
            this.hasContent = hasContent;
        }

        public List<NotificationDto> getContent() { return content; }
        public boolean isHasNext() { return hasNext; }
        public boolean isHasContent() { return hasContent; }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Cập nhật conversation notification cho realtime
     */
    private void updateConversationNotification(UUID userId, UUID conversationId, UUID messageId, 
                                              String messageContent, String senderName) {
        ConversationNotificationDto conversationNotification = ConversationNotificationDto.builder()
                .conversationId(conversationId)
                .lastMessageId(messageId)
                .lastMessageContent(messageContent)
                .lastMessageSender(senderName)
                .lastMessageTime(Instant.now())
                .unreadCount(1L) // Simplified for now
                .notificationType("NEW_MESSAGE")
                .build();

        // Send to user's conversation updates channel
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/conversation-updates", conversationNotification);
    }

    /**
     * Gửi notification real-time
     */
    private void sendRealtimeNotification(UUID userId, NotificationDto notification) {
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", notification);
    }

    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .metadata(deserializeMetadata(notification.getMetadata()))
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
            return null;
        }
    }

    private Map<String, Object> deserializeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize metadata", e);
            return new HashMap<>();
        }
    }

    private void clearUserNotificationCache(UUID userId) {
        Set<String> keys = redisTemplate.keys("*notifications*:" + userId + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}