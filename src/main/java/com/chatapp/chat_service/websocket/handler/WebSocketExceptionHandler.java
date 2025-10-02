package com.chatapp.chat_service.websocket.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Map;

@ControllerAdvice
public class WebSocketExceptionHandler {
    private SimpMessagingTemplate simpMessagingTemplate;
    @MessageExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public void handleAuthenticationException(
            AuthenticationCredentialsNotFoundException ex,
            StompHeaderAccessor accessor
    ) {
        String sessionId = accessor.getSessionId();
        String errorMessage = "Authentication failed: " + ex.getMessage();

        // Gửi lỗi về client qua session
        simpMessagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/errors",
                Map.of("error", errorMessage)
        );
    }

    @Autowired
    public void setSimpMessagingTemplate(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }
}