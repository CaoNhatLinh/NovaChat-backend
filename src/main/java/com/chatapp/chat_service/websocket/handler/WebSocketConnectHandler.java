package com.chatapp.chat_service.websocket.handler;

import com.chatapp.chat_service.security.JwtService;
import com.chatapp.chat_service.service.websocket.WebSocketConnectionService;
import com.chatapp.chat_service.service.presence.OnlineStatusService;
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
    private final OnlineStatusService onlineStatusService;
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
                onlineStatusService.setUserOnline(userId);

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