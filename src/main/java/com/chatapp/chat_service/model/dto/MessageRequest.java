package com.chatapp.chat_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private UUID replyTo;
    private String type; // "TEXT", "IMAGE", "VIDEO", "AUDIO", "FILE", "JOIN", "LEAVE", etc.
    private List<UUID> mentionedUserIds;
    
    // File attachment information
    private List<FileAttachment> attachments;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileAttachment {
        private String url;          // Cloudinary URL
        private String fileName;     // Original file name
        private String contentType;  // MIME type
        private Long fileSize;       // File size in bytes
        private String resourceType; // "image", "video", "audio", "raw"
        private String publicId;     // Cloudinary public ID for deletion
        private String thumbnailUrl; // For images
        private String mediumUrl;    // For images
    }
}
