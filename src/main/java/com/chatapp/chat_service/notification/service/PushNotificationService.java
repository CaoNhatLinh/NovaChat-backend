package com.chatapp.chat_service.notification.service;


import com.chatapp.chat_service.notification.dto.NotificationDto;
import com.chatapp.chat_service.websocket.handler.presence.WebSocketPresenceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PushNotificationService {
    private final WebSocketPresenceHandler webSocketPresenceHandler;

    public void sendPushNotification(NotificationDto notification) {
        // Implementation to send push notification
        // Could be Firebase Cloud Messaging, Apple Push Notification Service, etc.
        // Currently using WebSocket as example
        webSocketPresenceHandler.sendNotificationToUser(
                notification.getUserId().toString(),
                notification
        );
    }
}