package com.chatapp.chat_service.websocket.event;

import com.chatapp.chat_service.model.dto.MessageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEvent {
    // Existing fields for Kafka processing
    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private Instant createdAt;
    private List<UUID> mentionedUserIds;
    private UUID replyTo;
    private Instant timestamp;
    
    // NEW: Fields for WebSocket JSON format
    private String type; // "NEW_MESSAGE"
    private MessagePayload payload;
    
    // NEW: MessageRequest for Kafka processing
    private MessageRequest messageRequest;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessagePayload {
        private UUID conversationId;
        private String type; // Changed from messageType to type: "message", "image", "file", "join", "leave", etc.
        private String content;
        private List<UUID> mentions; // Array of userIds being mentioned
        private UUID replyTo; // UUID of message being replied to
        private List<MessageRequest.FileAttachment> attachments; // Array of file attachment objects
    }
    
    // Factory method to create from WebSocket JSON format
    public static MessageEvent fromWebSocketPayload(String type, MessagePayload payload, UUID senderId) {
        return MessageEvent.builder()
                .type(type)
                .payload(payload)
                .conversationId(payload.getConversationId())
                .senderId(senderId)
                .content(payload.getContent())
                .mentionedUserIds(payload.getMentions())
                .replyTo(payload.getReplyTo())
                .timestamp(Instant.now())
                .build();
    }
    
    // Factory method to create for Kafka processing
    public static MessageEvent forKafkaProcessing(MessageRequest messageRequest) {
        return MessageEvent.builder()
                .messageRequest(messageRequest)
                .type(messageRequest.getType())
                .conversationId(messageRequest.getConversationId())
                .senderId(messageRequest.getSenderId())
                .content(messageRequest.getContent())
                .mentionedUserIds(messageRequest.getMentionedUserIds())
                .replyTo(messageRequest.getReplyTo())
                .timestamp(Instant.now())
                .build();
    }
}
