package com.chatapp.chat_service.presence.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

import com.chatapp.chat_service.presence.entity.UserPresence;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresenceResponse {
    private UUID userId;
    private String status;        // ONLINE, OFFLINE, AWAY
    private String lastActiveAgo; // "3 phút trước" / null nếu đang online
    private Instant lastSeen;     // Raw timestamp
    private boolean isOnline;     // Convenience field
    
    public static UserPresenceResponse fromEntity(UserPresence presence) {
        if (presence == null) {
            return null;
        }
        
        return UserPresenceResponse.builder()
            .userId(presence.getUserId())
            .isOnline(presence.isOnline())
            .lastActiveAgo(presence.isOnline() ? null : formatLastActive(presence.getLastActive()))
            .build();
    }

    private static String formatLastActive(Instant lastActive) {
        if (lastActive == null) {
            return "Không rõ";
        }

        long secondsAgo = Instant.now().getEpochSecond() - lastActive.getEpochSecond();

        if (secondsAgo < 60) {
            return "Vừa mới";
        } else if (secondsAgo < 3600) {
            long minutes = secondsAgo / 60;
            return minutes + " phút trước";
        } else if (secondsAgo < 86400) {
            long hours = secondsAgo / 3600;
            return hours + " giờ trước";
        } else {
            long days = secondsAgo / 86400;
            return days + " ngày trước";
        }
    }
}
