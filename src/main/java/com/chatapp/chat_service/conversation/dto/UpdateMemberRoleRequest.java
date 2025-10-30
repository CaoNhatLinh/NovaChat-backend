package com.chatapp.chat_service.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRoleRequest {
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(admin|moderator|member)$", message = "Role must be: admin, moderator, or member")
    private String role;
}
