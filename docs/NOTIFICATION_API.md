# Notification System API Documentation

## 🔔 **Notification System Features**

### **Chức năng chính:**
1. ✅ Hiển thị danh sách thông báo theo thời gian (mới nhất đầu tiên)
2. ✅ Đếm số thông báo chưa đọc
3. ✅ Lọc theo loại thông báo
4. ✅ Real-time notifications qua WebSocket
5. ✅ Tự động cập nhật danh sách conversation và last_message
6. ✅ Redis caching để tối ưu performance
7. ✅ Kafka integration cho notification events

---

## 📡 **REST API Endpoints**

### **1. Lấy danh sách notifications**
```http
GET /api/notifications?page=0&size=20
Authorization: Bearer {token}

Response:
{
  "content": [
    {
      "notificationId": "uuid",
      "userId": "uuid", 
      "title": "Tin nhắn mới từ John",
      "body": "Hello, how are you?",
      "type": "MESSAGE",
      "metadata": {
        "conversationId": "uuid",
        "messageId": "uuid",
        "senderName": "John"
      },
      "isRead": false,
      "createdAt": "2025-01-14T10:30:00Z"
    }
  ],
  "hasNext": true,
  "hasContent": true
}
```

### **2. Lấy notifications chưa đọc**
```http
GET /api/notifications/unread
Authorization: Bearer {token}

Response:
[
  {
    "notificationId": "uuid",
    "title": "Bạn được mention", 
    "body": "@username check this out!",
    "type": "MENTION",
    "isRead": false,
    "createdAt": "2025-01-14T10:25:00Z"
  }
]
```

### **3. Đếm notifications chưa đọc**
```http
GET /api/notifications/unread/count
Authorization: Bearer {token}

Response:
{
  "count": 5
}
```

### **4. Lọc theo loại notification**
```http
GET /api/notifications/type/MESSAGE?page=0&size=10
Authorization: Bearer {token}

Response: {same as endpoint 1 but filtered by type}
```

### **5. Đánh dấu đã đọc**
```http
PUT /api/notifications/{notificationId}/read
Authorization: Bearer {token}

Response: 200 OK
```

### **6. Đánh dấu tất cả đã đọc**
```http
PUT /api/notifications/read-all
Authorization: Bearer {token}

Response: 200 OK
```

---

## 🔌 **WebSocket Events**

### **Subscribe để nhận notifications real-time:**
```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    // Subscribe to personal notifications
    stompClient.subscribe('/user/queue/notifications', function(notification) {
        const data = JSON.parse(notification.body);
        console.log('New notification:', data);
        updateNotificationUI(data);
    });
    
    // Subscribe to conversation updates
    stompClient.subscribe('/user/queue/conversation-updates', function(update) {
        const data = JSON.parse(update.body);
        console.log('Conversation update:', data);
        updateConversationList(data);
    });
    
    // Subscribe to notification read events
    stompClient.subscribe('/user/queue/notification-read', function(update) {
        const data = JSON.parse(update.body);
        console.log('Notification read:', data);
        markNotificationAsRead(data.notificationId);
    });
});
```

### **Send WebSocket messages:**
```javascript
// Mark notification as read
stompClient.send('/app/notification.read', {}, JSON.stringify({
    notificationId: 'uuid'
}));

// Mark all notifications as read
stompClient.send('/app/notifications.read-all', {}, '{}');
```

---

## 🎯 **Notification Types**

```javascript
const NotificationType = {
    MESSAGE: 'MESSAGE',           // Tin nhắn mới
    MENTION: 'MENTION',          // Được mention
    REACTION: 'REACTION',        // Reaction trên tin nhắn
    FRIEND_REQUEST: 'FRIEND_REQUEST',     // Lời mời kết bạn
    CONVERSATION_INVITE: 'CONVERSATION_INVITE', // Mời vào conversation
    SYSTEM: 'SYSTEM',            // Thông báo hệ thống
    POLL: 'POLL',                // Thông báo về poll
    PIN_MESSAGE: 'PIN_MESSAGE'   // Tin nhắn được pin
};
```

---

## 🏗️ **Implementation Examples**

### **Frontend Integration (React/Vue/Angular):**

```typescript
// Notification Service
class NotificationService {
    private stompClient: any;
    private notifications: Notification[] = [];
    private unreadCount: number = 0;

    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, () => {
            this.subscribeToNotifications();
        });
    }

    subscribeToNotifications() {
        // Real-time notifications
        this.stompClient.subscribe('/user/queue/notifications', (message) => {
            const notification = JSON.parse(message.body);
            this.addNotification(notification);
            this.showToast(notification);
        });

        // Conversation updates
        this.stompClient.subscribe('/user/queue/conversation-updates', (message) => {
            const update = JSON.parse(message.body);
            this.updateConversationList(update);
        });
    }

    async loadNotifications(page = 0, size = 20) {
        const response = await fetch(`/api/notifications?page=${page}&size=${size}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        return response.json();
    }

    async markAsRead(notificationId: string) {
        await fetch(`/api/notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });
    }

    async getUnreadCount() {
        const response = await fetch('/api/notifications/unread/count', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await response.json();
        return data.count;
    }
}
```

### **Backend Integration trong MessageService:**

```java
@Service
public class MessageService {
    
    private final NotificationService notificationService;
    
    public MessageResponse sendMessage(MessageRequest request) {
        // Save message
        Message message = saveMessage(request);
        
        // Get conversation members
        List<UUID> members = getConversationMembers(request.getConversationId());
        
        // Send notifications to all members except sender
        members.stream()
            .filter(memberId -> !memberId.equals(request.getSenderId()))
            .forEach(memberId -> {
                notificationService.createMessageNotification(
                    memberId,
                    request.getConversationId(),
                    message.getMessageId(),
                    getSenderName(request.getSenderId()),
                    request.getContent()
                );
            });
        
        return mapToResponse(message);
    }
}
```

---

## 🚀 **Performance Features**

1. **Redis Caching**: Notifications được cache để giảm load database
2. **Kafka Integration**: Notification events được gửi qua Kafka
3. **Real-time Updates**: WebSocket cho notifications instant
4. **Pagination**: Hỗ trợ phân trang cho danh sách notifications
5. **Efficient Queries**: Cassandra clustering key được tối ưu cho time-series data
6. **Batch Operations**: Mark all as read với single query

---

## 🔧 **Configuration**

### **Cassandra Schema:**
```sql
CREATE TABLE notifications (
    user_id UUID,
    notification_id UUID,
    title TEXT,
    body TEXT,
    type TEXT,
    metadata TEXT,
    is_read BOOLEAN,
    created_at TIMESTAMP,
    PRIMARY KEY (user_id, notification_id)
) WITH CLUSTERING ORDER BY (notification_id DESC)
AND compaction = {'class': 'TimeWindowCompactionStrategy', 'compaction_window_unit': 'DAYS', 'compaction_window_size': 1};
```

### **Redis Cache Keys:**
- `user_notifications:{userId}:{page}:{size}` - Paginated notifications
- `unread_notifications:{userId}` - Unread notifications list
- `unread_count:{userId}` - Unread count cache
- `conversation_notification:{userId}:{conversationId}` - Conversation updates

### **Kafka Topics:**
- `notification-topic` - Notification events
- `message-topic` - Message events (triggers notifications)
- `message-reaction-topic` - Reaction events
- `message-read-topic` - Read receipt events

---

## ✅ **Testing**

```bash
# Test notification creation
curl -X POST http://localhost:8084/api/notifications/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "title": "Test Notification",
    "body": "This is a test",
    "type": "SYSTEM",
    "metadata": {"test": true}
  }'

# Test get notifications
curl -X GET http://localhost:8084/api/notifications \
  -H "Authorization: Bearer {token}"

# Test unread count
curl -X GET http://localhost:8084/api/notifications/unread/count \
  -H "Authorization: Bearer {token}"
```

Hệ thống notification này cung cấp đầy đủ tính năng real-time, caching, và tích hợp với message system để tự động cập nhật conversation list! 🎉
