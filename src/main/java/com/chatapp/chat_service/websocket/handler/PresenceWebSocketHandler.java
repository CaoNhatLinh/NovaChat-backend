package com.chatapp.chat_service.websocket.handler;

//import com.chatapp.chat_service.service.MessageService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceWebSocketHandler extends TextWebSocketHandler {
//    private final MessageService messageService;
    private final Map<UUID, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

//    public PresenceWebSocketHandler(MessageService messageService) {
//        this.messageService = messageService;
//    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = (UUID) session.getAttributes().get("userId");
        if (userId != null) {
            activeSessions.put(userId, session);
//            messageService.setUserOnlineStatus(userId, true);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID userId = (UUID) session.getAttributes().get("userId");
        if (userId != null) {
            activeSessions.remove(userId);
//            messageService.setUserOnlineStatus(userId, false);
        }
    }

    public void sendNotificationToUser(String userId, Object notification) {
        WebSocketSession session = activeSessions.get(UUID.fromString(userId));
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(notification.toString()));
            } catch (IOException e) {
                // Handle exception
            }
        }
    }
}