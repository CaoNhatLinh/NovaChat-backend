package com.chatapp.chat_service.notification.service;
//package com.chatapp.chat_service.notification;
//
//import com.chatapp.chat_service.exception.NotFoundException;
//import com.chatapp.chat_service.model.dto.NotificationDTO;
//import com.chatapp.chat_service.model.entity.Notification;
//import com.chatapp.chat_service.model.entity.User;
//import com.chatapp.chat_service.model.key.NotificationKey;
//import com.chatapp.chat_service.repository.NotificationRepository;
//import com.chatapp.chat_service.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Slice;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Instant;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class InAppNotificationService {
//    private final NotificationRepository notificationRepository;
//    private final SimpMessagingTemplate messagingTemplate;
//    private final UserRepository userRepository;
//
//    /**
//     * Gửi thông báo trong ứng dụng và real-time qua WebSocket
//     */
//    @Transactional
//    public void sendInAppNotification(NotificationDTO notificationDTO) {
//        // Validate recipient
//        User user = userRepository.findById(notificationDTO.getUserId())
//                .orElseThrow(() -> new NotFoundException("User not found with id: " + notificationDTO.getUserId()));
//
//        // Build và lưu notification entity
//        NotificationKey key = new NotificationKey();
//        key.setUserId(user.getUser_id());
//        key.setNotificationId(notificationDTO.getNotificationId() != null ?
//                notificationDTO.getNotificationId() : UUID.randomUUID());
//        Notification notification = Notification.builder()
//                .key(key)
//                .title(notificationDTO.getTitle())
//                .body(notificationDTO.getBody())
//                .type(notificationDTO.getType())
//                .metadata(notificationDTO.getMetadata())
//                .isRead(notificationDTO.isRead())
//                .createdAt(notificationDTO.getCreatedAt() != null ?
//                        notificationDTO.getCreatedAt() : Instant.now())
//                .build();
//
//        Notification savedNotification = notificationRepository.save(notification);
//
//        // Gửi real-time qua WebSocket
//        sendRealTimeNotification(savedNotification);
//    }
//
//    /**
//     * Gửi thông báo real-time qua WebSocket
//     */
//    private void sendRealTimeNotification(Notification notification) {
//        String destination = "/topic/user/" + notification.getKey().getUserId() + "/notifications";
//        messagingTemplate.convertAndSend(destination, convertToDTO(notification));
//    }
//
//    /**
//     * Chuyển đổi từ Entity sang DTO
//     */
//    private NotificationDTO convertToDTO(Notification notification) {
//        return NotificationDTO.builder()
//
//                .notificationId(notification.getKey().getNotificationId())
//                .userId(notification.getKey().getUserId())
//                .title(notification.getTitle())
//                .body(notification.getBody())
//                .type(notification.getType())
//                .metadata(notification.getMetadata())
//                .isRead(notification.isRead())
//                .createdAt(notification.getCreatedAt())
//                .build();
//    }
//
//    /**
//     * Lấy tất cả thông báo của user
//     */
//    public Slice<NotificationDTO> getUserNotifications(UUID userId, Pageable pageable) {
//        return notificationRepository.findByUserId(userId, pageable)
//                .map(this::convertToDTO);
//    }
//
//    /**
//     * Đánh dấu thông báo đã đọc
//     */
//    @Transactional
//    public void markAsRead(UUID notificationId) {
//        notificationRepository.findById(
//                        NotificationKey.builder()
//                                .userId(notificationId)
//                                .notificationId(notificationId)
//                                .build())
//                .ifPresent(notification -> {
//                    notification.setRead(true);
//                    notificationRepository.save(notification);
//                });
//    }
//}