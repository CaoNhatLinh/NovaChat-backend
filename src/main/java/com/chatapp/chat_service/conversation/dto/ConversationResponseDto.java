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
public class ConversationResponseDto {
    private UUID conversationId;
    private String name;
    private String type;
    private String description;
    private UUID createdBy;
    private String backgroundUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;
    
    // For DM conversations - information about the other participant
    private UserProfileDto otherParticipant;
    
    // Last message information
    private LastMessageDto lastMessage;
    
    // Member count for groups
    private Integer memberCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileDto {
        private UUID userId;
        private String username;
        private String displayName;
        private String email;
        private String avatarUrl;
        private boolean isOnline;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastMessageDto {
        private UUID messageId;
        private UUID senderId;
        private String senderName;
        private String content;
        private String messageType;
        private Instant createdAt;
    }
}
