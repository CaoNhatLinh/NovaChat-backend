package com.chatapp.chat_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionDto {
    private UUID messageId;
    private String emoji; // đổi từ "type" thành "emoji"
    private LocalDateTime createdAt;
    private UserDTO user;
}
