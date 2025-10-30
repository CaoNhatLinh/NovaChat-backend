package com.chatapp.chat_service.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.file.dto.ImageDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDto {
    private UUID messageId;
    private UUID conversationId;
    private UserDTO sender;
    private String content;
    private List<String> mentionedUsers;
    private String messageType; // TEXT | IMAGE | VIDEO | AUDIO | FILE | JOIN | LEAVE
    private List<MessageAttachmentDto> attachments;
    private List<ImageDto> images;
    private List<MessageReactionDto> reactions;
    private ReplyToDto replyTo;
    private String replyType; // Message | ...
    private boolean isForwarded;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // File attachments from Cloudinary
    private List<FileAttachmentDto> fileAttachments;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileAttachmentDto {
        private String url;          // Cloudinary URL
        private String fileName;     // Original file name
        private String contentType;  // MIME type
        private Long fileSize;       // File size in bytes
        private String resourceType; // "image", "video", "audio", "raw"
        private String publicId;     // Cloudinary public ID
        private String thumbnailUrl; // For images
        private String mediumUrl;    // For images
        private String format;       // File format (jpg, mp4, etc.)
    }
}
