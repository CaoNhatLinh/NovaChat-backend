package com.chatapp.chat_service.presence.entity;

import lombok.*;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("user_presence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresence {

    public enum PrivacyMode {
        PUBLIC, // Ai cũng thấy
        FRIENDS_ONLY, // Chỉ bạn bè thấy
        HIDDEN // Tắt hoạt động
    }

    @PrimaryKey("user_id")
    private UUID userId;

    @Column("is_online")
    private boolean isOnline;
    
    @Column("last_active")
    private Instant lastActive;
    
    @Builder.Default
    @Column("privacy_mode")
    private String privacyMode = PrivacyMode.PUBLIC.name(); // Mặc định là PUBLIC

    // Helper
    public boolean isHidden() {
        return PrivacyMode.HIDDEN.name().equals(this.privacyMode);
    }
}