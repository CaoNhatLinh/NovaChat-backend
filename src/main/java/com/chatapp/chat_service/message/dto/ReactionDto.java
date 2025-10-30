package com.chatapp.chat_service.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.chatapp.chat_service.auth.dto.UserSummaryDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionDto {
    private UUID reactionId;
    private String emoji;
    private UserSummaryDto user;
}
