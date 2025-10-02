# 🔔 Complete NotificationService API Reference

## 📋 **All Available Methods**

### **1. Core Notification Methods**

#### **Create Notifications:**
```java
// Basic notification creation
NotificationDto createNotification(UUID userId, String title, String body, String type, Map<String, Object> metadata)

// Specific notification types
void createMessageNotification(UUID recipientId, UUID conversationId, UUID messageId, String senderName, String messageContent)
void createReactionNotification(UUID recipientId, UUID reactorId, String reactorName, String emoji, UUID conversationId, UUID messageId)
void createMentionNotification(UUID recipientId, UUID mentionerId, String mentionerName, UUID conversationId, UUID messageId, String messageContent)
void createFriendRequestNotification(UUID recipientId, UUID requesterId, String requesterName)
void createConversationInviteNotification(UUID recipientId, UUID inviterId, String inviterName, UUID conversationId, String conversationName)
void sendFriendshipUpdateNotification(UUID recipientId, UUID senderId, String status)
void createPollNotification(UUID recipientId, UUID creatorId, String creatorName, UUID conversationId, UUID pollId, String pollQuestion)
void createPinMessageNotification(UUID recipientId, UUID pinnerId, String pinnerName, UUID conversationId, UUID messageId, String messageContent)
void createSystemNotification(UUID recipientId, String title, String body, Map<String, Object> metadata)
```

#### **Query Notifications:**
```java
// Get notifications with pagination
NotificationPage getNotifications(UUID userId, int page, int size)
NotificationPage getNotifications(UUID userId, Pageable pageable)
NotificationPage getNotificationsByType(UUID userId, String type, int page, int size)

// Get specific notifications
List<NotificationDto> getUnreadNotifications(UUID userId)
Optional<NotificationDto> getLatestNotification(UUID userId)
List<NotificationDto> getNotificationsByDateRange(UUID userId, Instant startDate, Instant endDate)
List<NotificationDto> searchNotifications(UUID userId, String searchTerm, int limit)

// Get counts and statistics
Long getUnreadCount(UUID userId)
boolean hasUnreadNotifications(UUID userId)
NotificationStatsDto getNotificationStats(UUID userId)
```

#### **Update/Delete Notifications:**
```java
// Mark as read
void markAsRead(UUID userId, UUID notificationId)
void markAllAsRead(UUID userId)
void bulkMarkAsRead(UUID userId, List<UUID> notificationIds)

// Delete notifications
void deleteNotification(UUID userId, UUID notificationId)
void deleteAllNotifications(UUID userId)
```

---

## 🚀 **Usage Examples**

### **1. Creating Different Notification Types:**

```java
@RestController
@RequestMapping("/api/example")
@RequiredArgsConstructor
public class NotificationExampleController {
    
    private final NotificationService notificationService;
    
    @PostMapping("/message")
    public ResponseEntity<String> sendMessageNotification() {
        // When user sends a message
        notificationService.createMessageNotification(
            UUID.fromString("recipient-id"),
            UUID.fromString("conversation-id"),
            UUID.fromString("message-id"),
            "John Doe",
            "Hey, how are you doing?"
        );
        return ResponseEntity.ok("Message notification sent");
    }
    
    @PostMapping("/reaction")
    public ResponseEntity<String> sendReactionNotification() {
        // When user reacts to a message
        notificationService.createReactionNotification(
            UUID.fromString("message-owner-id"),
            UUID.fromString("reactor-id"),
            "Jane Smith",
            "❤️",
            UUID.fromString("conversation-id"),
            UUID.fromString("message-id")
        );
        return ResponseEntity.ok("Reaction notification sent");
    }
    
    @PostMapping("/mention")
    public ResponseEntity<String> sendMentionNotification() {
        // When user is mentioned
        notificationService.createMentionNotification(
            UUID.fromString("mentioned-user-id"),
            UUID.fromString("mentioner-id"),
            "Bob Wilson",
            UUID.fromString("conversation-id"),
            UUID.fromString("message-id"),
            "@john check this out! This is important."
        );
        return ResponseEntity.ok("Mention notification sent");
    }
    
    @PostMapping("/friend-request")
    public ResponseEntity<String> sendFriendRequestNotification() {
        // When someone sends friend request
        notificationService.createFriendRequestNotification(
            UUID.fromString("recipient-id"),
            UUID.fromString("requester-id"),
            "Alice Brown"
        );
        return ResponseEntity.ok("Friend request notification sent");
    }
    
    @PostMapping("/friend-update/{status}")
    public ResponseEntity<String> sendFriendshipUpdate(@PathVariable String status) {
        // When friend request status changes
        notificationService.sendFriendshipUpdateNotification(
            UUID.fromString("requester-id"),
            UUID.fromString("recipient-id"),
            status.toUpperCase() // ACCEPTED, REJECTED, PENDING
        );
        return ResponseEntity.ok("Friendship update notification sent");
    }
    
    @PostMapping("/poll")
    public ResponseEntity<String> sendPollNotification() {
        // When new poll is created
        notificationService.createPollNotification(
            UUID.fromString("recipient-id"),
            UUID.fromString("creator-id"),
            "Team Lead",
            UUID.fromString("conversation-id"),
            UUID.fromString("poll-id"),
            "What should we order for lunch?"
        );
        return ResponseEntity.ok("Poll notification sent");
    }
    
    @PostMapping("/pin")
    public ResponseEntity<String> sendPinNotification() {
        // When message is pinned
        notificationService.createPinMessageNotification(
            UUID.fromString("recipient-id"),
            UUID.fromString("pinner-id"),
            "Team Admin",
            UUID.fromString("conversation-id"),
            UUID.fromString("message-id"),
            "This is an important announcement that everyone should see"
        );
        return ResponseEntity.ok("Pin notification sent");
    }
    
    @PostMapping("/system")
    public ResponseEntity<String> sendSystemNotification() {
        // System announcements
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("priority", "HIGH");
        metadata.put("category", "MAINTENANCE");
        
        notificationService.createSystemNotification(
            UUID.fromString("user-id"),
            "System Maintenance",
            "The system will undergo maintenance from 2 AM to 4 AM",
            metadata
        );
        return ResponseEntity.ok("System notification sent");
    }
}
```

### **2. Querying Notifications:**

```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationQueryController {
    
    private final NotificationService notificationService;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<NotificationService.NotificationPage> getUserNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        NotificationService.NotificationPage notifications = 
            notificationService.getNotifications(userId, page, size);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<NotificationService.NotificationPage> getNotificationsByType(
            @PathVariable UUID userId,
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        NotificationService.NotificationPage notifications = 
            notificationService.getNotificationsByType(userId, type, page, size);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(@PathVariable UUID userId) {
        List<NotificationDto> unread = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(unread);
    }
    
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable UUID userId) {
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<NotificationStatsDto> getNotificationStats(@PathVariable UUID userId) {
        NotificationStatsDto stats = notificationService.getNotificationStats(userId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<NotificationDto> getLatestNotification(@PathVariable UUID userId) {
        Optional<NotificationDto> latest = notificationService.getLatestNotification(userId);
        return latest.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<List<NotificationDto>> searchNotifications(
            @PathVariable UUID userId,
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        
        String searchTerm = "%" + q + "%";
        List<NotificationDto> results = notificationService.searchNotifications(userId, searchTerm, limit);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<NotificationDto>> getNotificationsByDateRange(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        
        List<NotificationDto> notifications = 
            notificationService.getNotificationsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(notifications);
    }
}
```

### **3. Managing Notifications:**

```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationManagementController {
    
    private final NotificationService notificationService;
    
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/user/{userId}/bulk-read")
    public ResponseEntity<Void> bulkMarkAsRead(
            @PathVariable UUID userId,
            @RequestBody List<UUID> notificationIds) {
        
        notificationService.bulkMarkAsRead(userId, notificationIds);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID notificationId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.deleteNotification(userId, notificationId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/user/{userId}/all")
    public ResponseEntity<Void> deleteAllNotifications(@PathVariable UUID userId) {
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok().build();
    }
}
```

---

## 🔧 **Advanced Usage Patterns**

### **1. Batch Notification Creation:**
```java
@Service
public class BatchNotificationService {
    
    @Autowired
    private NotificationService notificationService;
    
    public void notifyConversationMembers(UUID conversationId, List<UUID> memberIds, 
                                        String senderName, String messageContent) {
        UUID messageId = UUID.randomUUID();
        
        // Send to all members except sender
        memberIds.parallelStream().forEach(memberId -> {
            notificationService.createMessageNotification(
                memberId, conversationId, messageId, senderName, messageContent
            );
        });
    }
    
    public void broadcastSystemNotification(List<UUID> userIds, String title, String body) {
        Map<String, Object> metadata = Map.of(
            "broadcast", true,
            "timestamp", Instant.now().toString()
        );
        
        userIds.parallelStream().forEach(userId -> {
            notificationService.createSystemNotification(userId, title, body, metadata);
        });
    }
}
```

### **2. Notification Analytics:**
```java
@Service
public class NotificationAnalyticsService {
    
    @Autowired
    private NotificationService notificationService;
    
    public Map<String, Object> getUserEngagementStats(UUID userId) {
        NotificationStatsDto stats = notificationService.getNotificationStats(userId);
        
        double readRate = stats.getTotalCount() > 0 ? 
            (double) stats.getReadCount() / stats.getTotalCount() * 100 : 0;
        
        return Map.of(
            "totalNotifications", stats.getTotalCount(),
            "readRate", Math.round(readRate * 100.0) / 100.0,
            "weeklyActivity", stats.getWeeklyCount(),
            "mostActiveType", getMostActiveNotificationType(stats.getTypeStats())
        );
    }
    
    private String getMostActiveNotificationType(Map<String, Long> typeStats) {
        return typeStats.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("NONE");
    }
}
```

### **3. Real-time Notification Dashboard:**
```javascript
// Frontend WebSocket integration
class NotificationDashboard {
    constructor(userId, token) {
        this.userId = userId;
        this.token = token;
        this.notifications = [];
        this.unreadCount = 0;
        this.connectWebSocket();
    }
    
    connectWebSocket() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({Authorization: 'Bearer ' + this.token}, () => {
            // Subscribe to personal notifications
            this.stompClient.subscribe('/user/queue/notifications', (message) => {
                const notification = JSON.parse(message.body);
                this.handleNewNotification(notification);
            });
            
            // Subscribe to read events
            this.stompClient.subscribe('/user/queue/notification-read', (message) => {
                const data = JSON.parse(message.body);
                this.handleNotificationRead(data);
            });
            
            // Subscribe to delete events
            this.stompClient.subscribe('/user/queue/notification-delete', (message) => {
                const data = JSON.parse(message.body);
                this.handleNotificationDelete(data);
            });
        });
    }
    
    handleNewNotification(notification) {
        this.notifications.unshift(notification);
        if (!notification.isRead) {
            this.unreadCount++;
        }
        this.updateUI();
        this.showToast(notification);
    }
    
    handleNotificationRead(data) {
        if (data.action === 'MARK_ALL_READ') {
            this.notifications.forEach(n => n.isRead = true);
            this.unreadCount = 0;
        } else if (data.notificationId) {
            const notification = this.notifications.find(n => n.notificationId === data.notificationId);
            if (notification && !notification.isRead) {
                notification.isRead = true;
                this.unreadCount--;
            }
        }
        this.updateUI();
    }
    
    async loadNotifications(page = 0, size = 20) {
        const response = await fetch(`/api/notifications/user/${this.userId}?page=${page}&size=${size}`, {
            headers: { 'Authorization': 'Bearer ' + this.token }
        });
        const data = await response.json();
        return data;
    }
    
    async markAsRead(notificationId) {
        await fetch(`/api/notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: { 'Authorization': 'Bearer ' + this.token }
        });
    }
    
    async getStats() {
        const response = await fetch(`/api/notifications/user/${this.userId}/stats`, {
            headers: { 'Authorization': 'Bearer ' + this.token }
        });
        return response.json();
    }
}
```

---

## 📊 **Performance Considerations**

### **1. Redis Caching Strategy:**
- **Notification Lists**: Cached for 10 minutes
- **Unread Counts**: Cached for 5 minutes  
- **User Stats**: Cached for 1 hour
- **Auto Cache Invalidation**: On any notification update

### **2. Database Optimization:**
- **Composite Primary Key**: (user_id, notification_id) for efficient querying
- **Clustering Order**: By notification_id DESC for latest-first retrieval
- **Time Window Compaction**: Optimized for time-series data patterns

### **3. Real-time Performance:**
- **WebSocket Channels**: User-specific queues for targeted delivery
- **Parallel Processing**: Batch operations use parallel streams
- **Async Operations**: Non-blocking notification creation

Với implementation này, bạn có một NotificationService hoàn chỉnh với tất cả chức năng cần thiết! 🎉
