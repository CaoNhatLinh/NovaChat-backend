package com.chatapp.chat_service.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberDto {
    private UUID userId;
    private UUID conversationId;
    private String role; // owner, admin, moderator, member
    private Instant joinedAt;
    
    // User details
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean isOnline;
    private Instant lastSeen;
}
