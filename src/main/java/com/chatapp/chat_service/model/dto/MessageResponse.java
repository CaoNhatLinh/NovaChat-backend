package com.chatapp.chat_service.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private Instant createdAt;
    private Instant editedAt;
    private boolean isDeleted;
    private String type; // Changed from messageType to type: "message", "image", "file", "join", "leave", etc.
    private String status; // Optional, can be used for display purposes
    private UUID replyTo;
    private List<UUID> mentionedUserIds;
}