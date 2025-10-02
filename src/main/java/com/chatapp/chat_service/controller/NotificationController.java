package com.chatapp.chat_service.controller;

import com.chatapp.chat_service.model.dto.NotificationDto;
import com.chatapp.chat_service.model.dto.NotificationStatsDto;
import com.chatapp.chat_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Lấy danh sách notifications với phân trang
     */
    @GetMapping
    public ResponseEntity<NotificationService.NotificationPage> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        NotificationService.NotificationPage notifications = notificationService.getNotifications(userId, page, size);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Lấy notifications chưa đọc
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<NotificationDto> unreadNotifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(unreadNotifications);
    }

    /**
     * Đếm notifications chưa đọc
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Lấy notifications theo loại
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<NotificationService.NotificationPage> getNotificationsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        NotificationService.NotificationPage notifications = notificationService.getNotificationsByType(userId, type, page, size);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Đánh dấu notification đã đọc
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * Đánh dấu tất cả notifications đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Tạo notification test (for development)
     */
    @PostMapping("/test")
    public ResponseEntity<NotificationDto> createTestNotification(
            @RequestBody Map<String, Object> request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        
        String title = (String) request.get("title");
        String body = (String) request.get("body");
        String type = (String) request.get("type");
        Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");
        
        NotificationDto notification = notificationService.createNotification(userId, title, body, type, metadata);
        return ResponseEntity.ok(notification);
    }

    /**
     * Xóa notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.deleteNotification(userId, notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * Xóa tất cả notifications
     */
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllNotifications(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Lấy thống kê notifications
     */
    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsDto> getNotificationStats(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        NotificationStatsDto stats = notificationService.getNotificationStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Đánh dấu nhiều notifications đã đọc
     */
    @PutMapping("/bulk-read")
    public ResponseEntity<Void> bulkMarkAsRead(
            @RequestBody List<UUID> notificationIds,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.bulkMarkAsRead(userId, notificationIds);
        return ResponseEntity.ok().build();
    }

    /**
     * Tìm kiếm notifications
     */
    @GetMapping("/search")
    public ResponseEntity<List<NotificationDto>> searchNotifications(
            @RequestParam String query,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        List<NotificationDto> notifications = notificationService.searchNotifications(userId, query, limit);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Lấy notifications theo khoảng thời gian
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<NotificationDto>> getNotificationsByDateRange(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        List<NotificationDto> notifications = notificationService.getNotificationsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Lấy notification mới nhất
     */
    @GetMapping("/latest")
    public ResponseEntity<NotificationDto> getLatestNotification(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return notificationService.getLatestNotification(userId)
                .map(notification -> ResponseEntity.ok(notification))
                .orElse(ResponseEntity.notFound().build());
    }
}
