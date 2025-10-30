package com.chatapp.chat_service.presence.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor // Lombok
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnlineStatusEvent {
    private UUID userId;
    
    @JsonProperty("online")
    private boolean online;
    
    private Instant timestamp;
    
    // Additional getter/setter for isOnline to maintain compatibility
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    // Also accept isOnline from JSON
    @JsonProperty("isOnline")
    public void setIsOnline(boolean isOnline) {
        this.online = isOnline;
    }
}