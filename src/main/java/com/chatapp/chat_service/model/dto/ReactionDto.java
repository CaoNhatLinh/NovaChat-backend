package com.chatapp.chat_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionDto {
    private UUID reactionId;
    private String emoji;
    private UserSummaryDto user;
}
