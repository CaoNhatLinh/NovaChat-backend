package com.chatapp.chat_service.notification;


import com.chatapp.chat_service.model.dto.NotificationDto;
import com.chatapp.chat_service.websocket.handler.PresenceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PushNotificationService {
    private final PresenceWebSocketHandler webSocketHandler;

    public void sendPushNotification(NotificationDto notification) {
        // Implementation to send push notification
        // Could be Firebase Cloud Messaging, Apple Push Notification Service, etc.
        // Currently using WebSocket as example
        webSocketHandler.sendNotificationToUser(
                notification.getUserId().toString(),
                notification
        );
    }
}