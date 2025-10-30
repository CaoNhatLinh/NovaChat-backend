package com.chatapp.chat_service.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewMessageEvent {
    private String type; // "NEW_MESSAGE"
    private NewMessagePayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewMessagePayload {
        private UUID conversationId;
        private String messageType; // "TEXT", "IMAGE", "FILE"
        private String content;
        private List<UUID> mentions; // Array of userIds being mentioned
        private UUID replyTo; // UUID of message being replied to
        private List<Object> attachments; // Array of attachment objects
    }
}
