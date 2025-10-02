package com.chatapp.chat_service.websocket.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private UUID notificationId;
    private UUID userId;
    private String title;
    private String body;
    private String type;
    private Map<String, Object> metadata;
    private Boolean isRead;
    private Instant createdAt;
    private String action; // "CREATE", "READ", "DELETE"
}