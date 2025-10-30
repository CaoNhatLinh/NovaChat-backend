package com.chatapp.chat_service.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để tạo invitation link
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvitationLinkRequest {
    private Long expiresInHours; // Số giờ link có hiệu lực (default: 24h)
    private Integer maxUses; // Số lần sử dụng tối đa (null = unlimited)
}
