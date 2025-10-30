package com.chatapp.chat_service.conversation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity cho quản lý lời mời vào conversation (group/channel)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("invitation_links")
public class InvitationLink {
    @PrimaryKey("link_id")
    private UUID linkId;
    
    private UUID conversationId;
    private String linkToken; // Token duy nhất cho link (để share)
    private UUID createdBy; // User tạo link
    private Instant createdAt;
    private Instant expiresAt; // Thời gian hết hạn
    private boolean isActive; // Có thể vô hiệu hóa link
    private Integer maxUses; // Số lần sử dụng tối đa (null = unlimited)
    private Integer usedCount; // Số lần đã sử dụng
}
