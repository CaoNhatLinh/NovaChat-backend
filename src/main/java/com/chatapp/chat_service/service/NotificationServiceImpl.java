//package com.chatapp.chat_service.service;
//
//import com.chatapp.chat_service.exception.ForbiddenException;
//import com.chatapp.chat_service.mapper.NotificationMapper;
//import com.chatapp.chat_service.model.dto.NotificationDTO;
//import com.chatapp.chat_service.model.entity.Notification;
//import com.chatapp.chat_service.model.key.NotificationKey;
//import com.chatapp.chat_service.notification.InAppNotificationService;
//import com.chatapp.chat_service.notification.PushNotificationService;
//import com.chatapp.chat_service.repository.NotificationRepository;
//import lombok.RequiredArgsConstructor;
//
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import org.springframework.data.domain.Pageable;
//import java.time.Instant;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class NotificationServiceImpl implements NotificationService {
//    private final NotificationRepository notificationRepository;
//    private final PushNotificationService pushNotificationService;
//    private final InAppNotificationService inAppNotificationService;
//    private final RedisTemplate<String, String> redisTemplate;
//
//    private static final String UNREAD_COUNT_KEY = "user:notifications:unread:%s";
//
//    @Override
//    public NotificationDTO createNotification(NotificationDTO notificationDTO) {
//        Notification notification = new Notification();
//        NotificationKey key = new NotificationKey();
//        key.setUserId(notificationDTO.getUserId());
//        key.setNotificationId(UUID.randomUUID());
//        notification.setKey(key);
//        notification.setTitle(notificationDTO.getTitle());
//        notification.setBody(notificationDTO.getBody());
//        notification.setType(notificationDTO.getType());
//        notification.setMetadata(notificationDTO.getMetadata());
//        notification.setCreatedAt(Instant.now());
//        notification.setRead(false);
//
//        Notification saved = notificationRepository.save(notification);
//
//        // Gửi push notification nếu user online
//        if (isUserOnline(notificationDTO.getUserId())) {
//            pushNotificationService.sendPushNotification(notificationDTO);
//        }
//
//        // Gửi in-app notification
//        inAppNotificationService.sendInAppNotification(notificationDTO);
//
//        // Tăng unread count trong Redis
//        incrementUnreadCount(notificationDTO.getUserId());
//
//        return NotificationMapper.toDTO(saved);
//    }
//
//    @Override
//    public List<NotificationDTO> getUserNotifications(UUID userId, Pageable pageable) {
//        return notificationRepository.findByUserId(userId, pageable)
//                .stream()
//                .map(NotificationMapper::toDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public void markAsRead(UUID notificationId, UUID userId) {
//        notificationRepository.findById(new NotificationKey(userId, notificationId))
//                .ifPresent(notification -> {
//                    if (!notification.getKey().getUserId().equals(userId)) {
//                        throw new ForbiddenException("You cannot mark this notification as read");
//                    }
//
//                    if (!notification.isRead()) {
//                        notification.setRead(true);
//                        notificationRepository.save(notification);
//
//                        // Giảm unread count trong Redis
//                        decrementUnreadCount(userId);
//                    }
//                });
//    }
//
//    @Override
//    public long getUnreadCount(UUID userId) {
//        String key = String.format(UNREAD_COUNT_KEY, userId);
//        String count = redisTemplate.opsForValue().get(key);
//
//        if (count == null) {
//            // Nếu không có trong Redis, lấy từ database và cache lại
//            long dbCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
//            redisTemplate.opsForValue().set(key, String.valueOf(dbCount));
//            return dbCount;
//        }
//
//        return Long.parseLong(count);
//    }
//
//    private void incrementUnreadCount(UUID userId) {
//        String key = String.format(UNREAD_COUNT_KEY, userId);
//        redisTemplate.opsForValue().increment(key);
//    }
//
//    private void decrementUnreadCount(UUID userId) {
//        String key = String.format(UNREAD_COUNT_KEY, userId);
//        Long count = redisTemplate.opsForValue().decrement(key);
//
//        if (count != null && count < 0) {
//            redisTemplate.opsForValue().set(key, "0");
//        }
//    }
//
//    private boolean isUserOnline(UUID userId) {
//        return Boolean.TRUE.equals(
//                redisTemplate.opsForSet().isMember("user:online", userId.toString())
//        );
//    }
//}