//package com.chatapp.chat_service.kafka.notification;
//
//import com.chatapp.chat_service.websocket.event.FriendRequestEvent;
//import com.chatapp.chat_service.websocket.event.NotificationEvent;
//import lombok.RequiredArgsConstructor;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class KafkaNotificationProducer {
//    private final KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate;
//    private final KafkaTemplate<String, FriendRequestEvent> friendRequestKafkaTemplate;
//
//    public void sendNotification(NotificationEvent event) {
//        notificationKafkaTemplate.send("notifications", event);
//    }
//
//    public void sendFriendRequestNotification(FriendRequestEvent event) {
//        friendRequestKafkaTemplate.send("friend-requests", event);
//    }
//}