package com.chatapp.chat_service.websocket;

//import com.chatapp.chat_service.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TypingHandler extends TextWebSocketHandler {
//    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public TypingHandler( ObjectMapper objectMapper) {
//        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = extractUserIdFromSession(session);
        if (userId != null) {
            sessions.put(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID userId = extractUserIdFromSession(session);
        if (userId != null) {
            // Handle typing events here
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID userId = extractUserIdFromSession(session);
        if (userId != null) {
            sessions.remove(userId);
        }
    }

    private UUID extractUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    String token = param.substring(6);
                    // Extract user ID from token (implement your JWT parsing logic)
                    return UUID.fromString(token); // Simplified example
                }
            }
        } catch (Exception e) {
            // Handle error
        }
        return null;
    }
}