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
public class UserSummaryDto {
    private UUID userId;
    private String firstName;
    private String fullName;
    private String username;
    private String avatar;
}
