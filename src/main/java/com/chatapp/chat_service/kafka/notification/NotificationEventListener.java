//package com.chatapp.chat_service.kafka.notification;
//
//import com.chatapp.chat_service.model.dto.NotificationDTO;
//import com.chatapp.chat_service.model.dto.NotificationMessage;
//import com.chatapp.chat_service.service.NotificationService;
//import com.chatapp.chat_service.websocket.event.FriendRequestEvent;
//import com.chatapp.chat_service.websocket.event.NotificationEvent;
//import lombok.RequiredArgsConstructor;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class NotificationEventListener {
//    private final NotificationService notificationService;
//
//    @KafkaListener(topics = "notifications", groupId = "notification-group")
//    public void handleNotificationEvent(NotificationEvent event) {
//        NotificationDTO dto = new NotificationDTO();
//        dto.setUserId(event.getUserId());
//        dto.setTitle(event.getTitle());
//        dto.setBody(event.getBody());
//        dto.setType(event.getType());
//        dto.setMetadata(event.getMetadata());
//
//        notificationService.createNotification(dto);
//    }
//
//    @KafkaListener(topics = "friend-requests", groupId = "notification-group")
//    public void handleFriendRequestEvent(FriendRequestEvent event) {
//        NotificationDTO dto = new NotificationDTO();
//        dto.setUserId(event.getReceiverId());
//        dto.setTitle("New Friend Request");
//        dto.setBody(String.format("%s sent you a friend request", event.getSenderName()));
//        dto.setType("FRIEND_REQUEST");
//
//        notificationService.createNotification(dto);
//    }
//}