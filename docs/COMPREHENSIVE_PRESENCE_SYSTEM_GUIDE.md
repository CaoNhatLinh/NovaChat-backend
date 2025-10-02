# Comprehensive Online Status System Documentation

## Overview
This document describes the enhanced online status system using Redis, Kafka, and Cassandra for real-time presence tracking with automatic timeout, session management, and scalable notification system.

## Architecture Components

### 🔴 Redis Layer
- **Purpose**: Fast real-time presence tracking and session management
- **Key Patterns**:
  - `presence:online` - Set of all online user IDs
  - `presence:user:{userId}:online:{sessionId}` - Individual session keys with TTL (60s)
  - `presence:user:{userId}:last_active` - Last active timestamp
  - `presence:user:{userId}:watchers` - Set of users watching this user's presence

### ⚪ Cassandra Layer  
- **Purpose**: Persistent presence storage and historical data
- **Table**: `user_presence`
  - `user_id` (Primary Key)
  - `is_online` (Boolean)
  - `last_active` (Timestamp)
  - `device` (String: "web", "mobile", "tablet")

### 🟡 Kafka Layer
- **Topic**: `user.presence.status`
- **Purpose**: Distribute presence change events across all service instances
- **Event**: `OnlineStatusEvent` with userId, online status, timestamp

### 🟢 WebSocket Layer
- **Channels**:
  - `/queue/online-status` - User-specific presence notifications
  - `/app/online-status` - Client presence updates
  - `/app/heartbeat` - Session refresh
  - `/app/presence.subscribe` - Subscribe to user presence
  - `/app/presence.unsubscribe` - Unsubscribe from user presence

## Flow Diagrams

### Online Flow
```
[1] Client connects WebSocket
    ↓
[2] WebSocketPresenceHandler detects connection
    ↓
[3] PresenceService.setUserOnline(userId, device, sessionId)
    ↓
[4] Redis: Create session key with 60s TTL + Add to online set
    ↓
[5] Cassandra: Update is_online = true, last_active
    ↓
[6] Kafka: Send OnlineStatusEvent(online=true)
    ↓
[7] OnlineStatusEventConsumer processes event
    ↓
[8] WebSocket: Notify all watchers via /queue/online-status
```

### Offline Flow (Disconnect)
```
[1] Client disconnects WebSocket
    ↓
[2] WebSocketPresenceHandler detects disconnection
    ↓
[3] PresenceService.setUserOffline(userId, sessionId)
    ↓
[4] Redis: Remove session key, check remaining sessions
    ↓
[5] If no more sessions: Remove from online set + Update last_active
    ↓
[6] Cassandra: Update is_online = false, last_active = now()
    ↓
[7] Kafka: Send OnlineStatusEvent(online=false)
    ↓
[8] Notify all watchers
```

### Offline Flow (Timeout)
```
[1] Redis session key expires (60s TTL)
    ↓
[2] RedisKeyExpirationListener detects expired key
    ↓
[3] Check if user has other active sessions
    ↓
[4] If no more sessions: Remove from online set
    ↓
[5] Kafka: Send OnlineStatusEvent(online=false)
    ↓
[6] Follow same notification flow as manual disconnect
```

### Heartbeat Flow
```
[1] Client sends periodic heartbeat (/app/heartbeat)
    ↓
[2] WebSocketChatController.handleHeartbeat()
    ↓
[3] PresenceService.refreshUserSession(userId, sessionId)
    ↓
[4] Redis: Extend session key TTL to 60s
    ↓
[5] Update last_active timestamp
```

## API Endpoints

### REST API (PresenceController)

#### Get Friends Presence
```http
GET /api/presence/friends
Authorization: Bearer {token}
```
**Response:**
```json
{
  "f1f8f884-a8aa-44ec-9313-e234860c5e41": {
    "userId": "f1f8f884-a8aa-44ec-9313-e234860c5e41",
    "status": "ONLINE",
    "isOnline": true,
    "lastSeen": null,
    "lastActiveAgo": null
  },
  "a2b3c4d5-e6f7-8901-2345-6789abcdef01": {
    "userId": "a2b3c4d5-e6f7-8901-2345-6789abcdef01", 
    "status": "OFFLINE",
    "isOnline": false,
    "lastSeen": "2025-07-17T10:30:00Z",
    "lastActiveAgo": "2 giờ trước"
  }
}
```

#### Get Conversation Members Presence
```http
GET /api/presence/conversation/{conversationId}
Authorization: Bearer {token}
```

#### Check Specific User Online
```http
GET /api/presence/check/{userId}
Authorization: Bearer {token}
```
**Response:**
```json
{
  "isOnline": true
}
```

#### Batch Presence Check
```http
POST /api/presence/batch
Authorization: Bearer {token}
Content-Type: application/json

[
  "f1f8f884-a8aa-44ec-9313-e234860c5e41",
  "a2b3c4d5-e6f7-8901-2345-6789abcdef01"
]
```

### WebSocket API

#### Client Presence Update
```javascript
stompClient.send("/app/online-status", {}, JSON.stringify({
  online: true
}));
```

#### Subscribe to Presence
```javascript
stompClient.send("/app/presence.subscribe", {}, JSON.stringify({
  userIds: ["f1f8f884-a8aa-44ec-9313-e234860c5e41", "..."]
}));
```

#### Heartbeat (Keep Session Alive)
```javascript
// Send every 30 seconds
setInterval(() => {
  stompClient.send("/app/heartbeat", {});
}, 30000);
```

#### Listen to Presence Updates
```javascript
stompClient.subscribe("/user/queue/online-status", (message) => {
  const event = JSON.parse(message.body);
  console.log(`User ${event.userId} is now ${event.online ? 'online' : 'offline'}`);
});
```

## Key Features

### ✅ Session-Based Tracking
- Multiple sessions per user (web, mobile, tablet)
- Each session has independent TTL
- User stays online while any session is active

### ✅ Automatic Timeout
- 60-second TTL on session keys
- Redis keyspace notifications for expired keys
- Graceful offline detection without client polling

### ✅ Real-Time Notifications
- Kafka-based event distribution
- WebSocket notifications to interested users
- Subscription management for watching specific users

### ✅ "Last Active" Tracking
- Redis stores millisecond-precision timestamps
- Formatted display ("2 phút trước", "3 giờ trước")
- Fallback to Cassandra for historical data

### ✅ Scalable Architecture
- Stateless service instances
- Redis for fast read/write operations
- Kafka for cross-instance communication
- Cassandra for persistence and analytics

### ✅ Multi-Device Support
- Device type detection from User-Agent
- Independent session management per device
- Accurate online/offline state across devices

## Configuration

### Redis Setup
```properties
# Enable keyspace notifications for expired keys
notify-keyspace-events Ex
```

### Application Properties
```properties
# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000ms
spring.redis.database=0

# Kafka Configuration  
spring.kafka.bootstrap-servers=localhost:9092
```

### Session TTL Settings
```java
// In PresenceService
private static final long SESSION_TTL = 60L; // seconds
```

## Monitoring & Maintenance

### Scheduled Tasks
- **Cleanup Task**: Runs every 5 minutes to sync Redis/Cassandra
- **Stats Logging**: Logs online user count every minute
- **Stale Detection**: Removes orphaned presence records

### Health Checks
```java
// Get current online count
int onlineCount = presenceService.getAllOnlineUserIds().size();

// Get user's active sessions
Set<String> sessions = presenceService.getUserActiveSessions(userId);

// Check specific user status
boolean isOnline = presenceService.isUserOnline(userId);
```

## Troubleshooting

### Common Issues

1. **User Shows Offline But Backend Says Online**
   - Check Redis `presence:online` set: `SMEMBERS presence:online`
   - Check session keys: `KEYS presence:user:{userId}:online:*`
   - Verify Kafka consumer is processing events

2. **Session Not Expiring**
   - Verify keyspace notifications: `CONFIG GET notify-keyspace-events`
   - Check RedisKeyExpirationListener is active
   - Monitor expired key events in logs

3. **Performance Issues**
   - Monitor Redis memory usage
   - Check Kafka consumer lag
   - Review session cleanup efficiency

### Debug Commands

```bash
# Redis debugging
redis-cli SMEMBERS presence:online
redis-cli KEYS "presence:user:*:online:*"
redis-cli GET "presence:user:{userId}:last_active"

# Check keyspace notifications
redis-cli CONFIG GET notify-keyspace-events
redis-cli PSUBSCRIBE "__keyevent@0__:expired"
```

## Frontend Integration

### Example Implementation
```javascript
class PresenceManager {
  constructor(stompClient) {
    this.stompClient = stompClient;
    this.heartbeatInterval = null;
  }
  
  start() {
    // Set online status
    this.stompClient.send("/app/online-status", {}, JSON.stringify({
      online: true
    }));
    
    // Start heartbeat
    this.heartbeatInterval = setInterval(() => {
      this.stompClient.send("/app/heartbeat", {});
    }, 30000); // 30 seconds
    
    // Listen for presence updates
    this.stompClient.subscribe("/user/queue/online-status", (message) => {
      const event = JSON.parse(message.body);
      this.updateUserPresence(event.userId, event.online);
    });
  }
  
  stop() {
    // Set offline status
    this.stompClient.send("/app/online-status", {}, JSON.stringify({
      online: false
    }));
    
    // Stop heartbeat
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }
  }
  
  subscribeToUsers(userIds) {
    this.stompClient.send("/app/presence.subscribe", {}, JSON.stringify({
      userIds: userIds
    }));
  }
}
```

This comprehensive system provides real-time, scalable, and reliable online status tracking with automatic timeout management and multi-device support.
