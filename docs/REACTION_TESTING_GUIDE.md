# 🎯 Test Toggle Reaction API

## 📋 **Testing Guide cho Toggle Reaction Feature**

### **Scenario 1: Add New Reaction**
```bash
# Test thêm reaction mới
curl -X POST http://localhost:8084/api/messages/{conversationId}/{messageId}/reactions/❤️ \
  -H "Authorization: Bearer {your-jwt-token}" \
  -H "Content-Type: application/json"

# Expected: 200 OK
# WebSocket sẽ broadcast: {"action": "ADD", "emoji": "❤️", "userId": "...", ...}
# Notification sẽ được tạo cho message owner
```

### **Scenario 2: Remove Existing Reaction**
```bash
# Test xóa reaction đã có (call lại endpoint giống như trên)
curl -X POST http://localhost:8084/api/messages/{conversationId}/{messageId}/reactions/❤️ \
  -H "Authorization: Bearer {your-jwt-token}" \
  -H "Content-Type: application/json"

# Expected: 200 OK  
# WebSocket sẽ broadcast: {"action": "REMOVE", "emoji": "❤️", "userId": "...", ...}
# Không có notification được tạo
```

### **Scenario 3: Get All Reactions for Message**
```bash
# Lấy danh sách tất cả reactions cho message
curl -X GET http://localhost:8084/api/messages/{conversationId}/{messageId}/reactions \
  -H "Authorization: Bearer {your-jwt-token}"

# Expected Response:
{
  "reactions": [
    {
      "emoji": "❤️",
      "count": 3,
      "userReacted": true,
      "users": [
        {"userId": "uuid1", "username": "John"},
        {"userId": "uuid2", "username": "Jane"},
        {"userId": "uuid3", "username": "Bob"}
      ]
    },
    {
      "emoji": "👍",
      "count": 1,
      "userReacted": false,
      "users": [
        {"userId": "uuid4", "username": "Alice"}
      ]
    }
  ]
}
```

---

## 🔧 **Advanced Testing with Multiple Users**

### **JavaScript WebSocket Test Client:**
```javascript
// Test real-time reaction updates
const socket = new SockJS('http://localhost:8084/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({Authorization: 'Bearer ' + token}, function() {
    // Subscribe to conversation reactions
    stompClient.subscribe('/topic/conversation/' + conversationId + '/reactions', function(event) {
        const reactionEvent = JSON.parse(event.body);
        console.log('Reaction Event:', reactionEvent);
        
        // Update UI based on action
        if (reactionEvent.action === 'ADD') {
            addReactionToUI(reactionEvent.emoji, reactionEvent.userId);
        } else if (reactionEvent.action === 'REMOVE') {
            removeReactionFromUI(reactionEvent.emoji, reactionEvent.userId);
        }
    });
    
    // Subscribe to personal notifications
    stompClient.subscribe('/user/queue/notifications', function(notification) {
        const notif = JSON.parse(notification.body);
        if (notif.type === 'REACTION') {
            showNotification(notif.title + ': ' + notif.body);
        }
    });
});

// Function to send reaction
function toggleReaction(conversationId, messageId, emoji) {
    fetch(`/api/messages/${conversationId}/${messageId}/reactions/${emoji}`, {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        }
    }).then(response => {
        if (response.ok) {
            console.log('Reaction toggled successfully');
        }
    });
}
```

---

## 🚀 **Performance Testing**

### **Load Test với Multiple Reactions:**
```bash
# Test với nhiều emoji khác nhau
emojis=("❤️" "👍" "😂" "😮" "😢" "😡")

for emoji in "${emojis[@]}"; do
    curl -X POST "http://localhost:8084/api/messages/{conversationId}/{messageId}/reactions/${emoji}" \
      -H "Authorization: Bearer {token}" &
done

wait # Chờ tất cả requests hoàn thành
```

### **Stress Test với nhiều users:**
```bash
# Simulate 100 users reacting to same message
for i in {1..100}; do
    curl -X POST "http://localhost:8084/api/messages/{conversationId}/{messageId}/reactions/❤️" \
      -H "Authorization: Bearer {user${i}-token}" &
    
    if [ $((i % 10)) -eq 0 ]; then
        wait # Chờ mỗi 10 requests
        echo "Completed $i reactions"
    fi
done
```

---

## 📊 **Expected Performance Metrics**

### **Response Times:**
- ✅ Toggle Reaction: < 50ms
- ✅ Get Reactions: < 30ms (with Redis cache)
- ✅ WebSocket Broadcast: < 10ms
- ✅ Notification Creation: < 100ms

### **Cache Efficiency:**
- ✅ Redis hit rate: > 90% for frequent messages
- ✅ Cache invalidation: Immediate on reaction changes
- ✅ Memory usage: Optimized with TTL

### **Real-time Features:**
- ✅ WebSocket latency: < 20ms
- ✅ Kafka processing: < 50ms
- ✅ Database writes: < 100ms

---

## 🐛 **Common Issues & Solutions**

### **Issue 1: JWT Token Expired**
```bash
# Error: 401 Unauthorized
# Solution: Refresh token
curl -X POST http://localhost:8084/api/auth/refresh \
  -H "Authorization: Bearer {refresh-token}"
```

### **Issue 2: Invalid Emoji**
```bash
# Error: 400 Bad Request - Invalid emoji format
# Solution: URL encode emojis
emoji_encoded=$(python3 -c "import urllib.parse; print(urllib.parse.quote('❤️'))")
curl -X POST "http://localhost:8084/api/messages/{conversationId}/{messageId}/reactions/${emoji_encoded}"
```

### **Issue 3: WebSocket Connection Failed**
```javascript
// Check connection status
if (stompClient.connected) {
    console.log('WebSocket connected');
} else {
    console.log('WebSocket disconnected - attempting reconnect...');
    setTimeout(() => stompClient.connect(), 1000);
}
```

---

## ✅ **Verification Checklist**

### **Functional Tests:**
- [ ] ✅ Add reaction to message
- [ ] ✅ Remove reaction from message  
- [ ] ✅ Multiple users react to same message
- [ ] ✅ User reacts with multiple emojis
- [ ] ✅ Get reactions list with counts
- [ ] ✅ Real-time WebSocket updates
- [ ] ✅ Notification to message owner
- [ ] ✅ No notification for self-reactions

### **Edge Cases:**
- [ ] ✅ React to non-existent message (404)
- [ ] ✅ React with invalid emoji
- [ ] ✅ React without authentication (401)
- [ ] ✅ React to message in private conversation
- [ ] ✅ Database connection failure handling
- [ ] ✅ Redis cache failure graceful degradation

### **Performance Tests:**
- [ ] ✅ 1000 concurrent reactions
- [ ] ✅ Redis cache hit rate > 90%
- [ ] ✅ WebSocket broadcast latency < 20ms
- [ ] ✅ Database query optimization
- [ ] ✅ Memory usage under load

Với implementation này, reaction system sẽ hoạt động mượt mà với real-time updates, notifications, và performance optimization! 🎉
