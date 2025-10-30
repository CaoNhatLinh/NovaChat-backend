package com.chatapp.chat_service.websocket.handler.connection;

import com.chatapp.chat_service.presence.service.PresenceService;
import com.chatapp.chat_service.security.jwt.JwtService;
import com.chatapp.chat_service.websocket.service.WebSocketConnectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectHandler {

    private final WebSocketConnectionService connectionService;
    private final PresenceService presenceService;
    private final JwtService jwtService;
    @EventListener
    public void handleWebSocketConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        String token = (authHeaders != null && !authHeaders.isEmpty())
                ? authHeaders.get(0).replace("Bearer ", "")
                : null;
        UUID userId = jwtService.getUserIdFromToken(token);
        if (userId != null) {
            try {
                connectionService.registerConnection(userId);
                presenceService.setUserOnlineStatus(userId, true);

            } catch (IllegalArgumentException e) {
                log.error("Invalid user-id format: {}", userId);
            }
        }
    }


    private UUID extractUserId(StompHeaderAccessor accessor) {
        String userIdHeader = accessor.getFirstNativeHeader("user-id");
        if (userIdHeader != null) {
            try {
                return UUID.fromString(userIdHeader);
            } catch (IllegalArgumentException e) {
                log.error("Invalid user-id format: {}", userIdHeader);
            }
        }
        return null;
    }

    private String extractDeviceFromHeaders(StompHeaderAccessor accessor) {

        String deviceHeader = accessor.getFirstNativeHeader("device");
        if (deviceHeader != null) {
            return deviceHeader;
        }
        return "web";
    }
}