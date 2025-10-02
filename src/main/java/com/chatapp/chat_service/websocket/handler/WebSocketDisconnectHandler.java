package com.chatapp.chat_service.websocket.handler;


import com.chatapp.chat_service.service.websocket.WebSocketConnectionService;
import com.chatapp.chat_service.service.presence.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketDisconnectHandler {

    private final WebSocketConnectionService connectionService;
    private final OnlineStatusService onlineStatusService;
    private final TaskScheduler taskScheduler;

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userIdHeader = accessor.getFirstNativeHeader("user-id");

        if (userIdHeader != null) {
            try {
                UUID userId = UUID.fromString(userIdHeader);
                log.info("User disconnected: {}", userId);

                // Hủy đăng ký kết nối
                connectionService.unregisterConnection(userId);

                // Hẹn giờ kiểm tra trạng thái offline
                taskScheduler.schedule(() -> {
                    if (!connectionService.hasActiveConnection(userId)) {
                        onlineStatusService.setUserOffline(userId);
                        log.info("User set to offline: {}", userId);
                    }
                }, Instant.now().plusSeconds(30));

            } catch (IllegalArgumentException e) {
                log.error("Invalid user-id format: {}", userIdHeader);
            }
        }
    }
}