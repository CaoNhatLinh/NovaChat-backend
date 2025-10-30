package com.chatapp.chat_service.presence.controller;

import com.chatapp.chat_service.presence.dto.UserPresenceResponse;
import com.chatapp.chat_service.presence.service.PresenceService;
import com.chatapp.chat_service.security.core.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;
    private final SecurityContextHelper securityContextHelper; // Helper để lấy user ID

    /**
     * API này được client gọi mỗi 30 GIÂY.
     * Nó chỉ gia hạn TTL trong Redis, cực kỳ nhanh.
     * @param sessionId ID của tab/thiết bị (frontend tự tạo)
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestParam String sessionId) {
        UUID userId = securityContextHelper.getCurrentUserId(); // Lấy user ID từ token
        presenceService.handleHeartbeat(userId, sessionId);
        return ResponseEntity.ok().build();
    }

    /**
     * API này được client gọi MỘT LẦN khi online.
     * Dùng để đồng bộ danh sách những người cần theo dõi.
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Void> syncSubscriptions(@RequestBody List<UUID> targetUserIds) {
        UUID userId = securityContextHelper.getCurrentUserId();
        presenceService.syncSubscriptions(userId, targetUserIds);
        return ResponseEntity.ok().build();
    }
    
    /**
     * API này được client gọi để LẤY 1 LẦN trạng thái của 1 list user.
     * (Ví dụ: khi mở danh sách bạn bè).
     */
    @PostMapping("/batch-get")
    public ResponseEntity<Map<UUID, UserPresenceResponse>> getBatchPresence(@RequestBody List<UUID> userIds) {
        // TODO: Cần 1 API "getFriendsPresence" riêng
        // API này tạm thời cho phép lấy bất kỳ ai
        Map<UUID, UserPresenceResponse> presence = presenceService.getBatchPresence(userIds);
        return ResponseEntity.ok(presence);
    }
}