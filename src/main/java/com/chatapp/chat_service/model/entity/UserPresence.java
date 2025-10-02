package com.chatapp.chat_service.model.entity;

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
    @PrimaryKey("user_id")
    private UUID userId;

    @Builder.Default
    @Column("is_online")
    private boolean isOnline = false;
    
    @Column("last_active")
    private Instant lastActive;
    
    private String device; // "web", "android", etc.

    
    // Helper methods
    public boolean isOnline() {
        return this.isOnline;
    }
    
    public void setOnline() {
        this.isOnline = true;
        this.lastActive = Instant.now(); // Update last active when going online
    }
    
    public void setOffline() {
        this.isOnline = false;
        this.lastActive = Instant.now(); // Set last active when going offline
    }
    
    // Get status as string for compatibility
    public String getStatus() {
        return this.isOnline ? "ONLINE" : "OFFLINE";
    }
    
    // Get lastSeen for compatibility (same as lastActive when offline)
    public Instant getLastSeen() {
        return this.isOnline ? null : this.lastActive;
    }
    
    public PresenceStatus getStatusEnum() {
        return this.isOnline ? PresenceStatus.ONLINE : PresenceStatus.OFFLINE;
    }
    
    public void setStatus(PresenceStatus status) {
        this.isOnline = (status == PresenceStatus.ONLINE);
        if (!this.isOnline) {
            this.lastActive = Instant.now();
        }
    }
    
    public enum PresenceStatus {
        ONLINE, OFFLINE, AWAY
    }
}
