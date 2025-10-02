# Comprehensive WebSocket Connection and Presence Management System

## Overview

Hệ thống quản lý kết nối WebSocket và presence tracking hoàn chỉnh với session-based tracking, heartbeat support, và automatic cleanup mechanisms. Hệ thống được thiết kế để hỗ trợ multi-device connections, real-time presence updates, và Redis key expiration handling.

## System Architecture

### 1. Core Services

#### WebSocketConnectionService
- **Location**: `com.chatapp.chat_service.service.websocket.WebSocketConnectionService`
- **Purpose**: Quản lý session-based WebSocket connections với TTL support
- **Key Features**:
  - Session tracking với 60s TTL
  - Multi-device support
  - Automatic cleanup of expired sessions
  - Device information tracking

#### PresenceService
- **Location**: `com.chatapp.chat_service.service.presence.PresenceService`
- **Purpose**: Quản lý user presence với session-based tracking
- **Key Features**:
  - Session-based online/offline tracking
  - Multi-device presence support
  - Presence watchers mechanism
  - Kafka event publishing

#### PresenceSubscriptionService
- **Location**: `com.chatapp.chat_service.service.subscription.PresenceSubscriptionService`
- **Purpose**: Quản lý subscription cho presence updates
- **Key Features**:
  - Subscribe/unsubscribe to user presence
  - TTL-based subscription management
  - Bi-directional subscription tracking

### 2. Redis Key Patterns

#### WebSocket Sessions
```
ws:session:{sessionId}          # Session data với TTL 60s
ws:user:sessions:{userId}       # Set of active sessions per user
ws:connections:{userId}         # Connection count (deprecated)
```

#### Presence Tracking
```
presence:online                 # Global set of online users
presence:user:{userId}:online:{sessionId}  # User session với TTL 60s
presence:user:{userId}:last_active         # Last active timestamp
presence:user:{userId}:watchers            # Set of users watching this user
```

#### Subscriptions
```
presence:subscription:{subscriberId}    # Set of subscribed users
presence:subscribers:{targetUserId}     # Set of subscribers for target user
```

### 3. API Endpoints

#### WebSocket APIs
```
/app/heartbeat                  # Heartbeat to refresh session TTL
/app/presence.subscribe         # Subscribe to presence updates
/app/presence.unsubscribe       # Unsubscribe from presence updates
/app/message.send              # Send message (existing)
/app/typing                    # Typing indicators (existing)
/app/online-status             # Online status updates (existing)
```

#### REST APIs
```
GET  /api/subscriptions/presence/{userId}                    # Get user's subscriptions
GET  /api/subscriptions/presence/{userId}/subscribers        # Get user's subscribers
GET  /api/subscriptions/presence/{subscriberId}/subscribed-to/{targetUserId}  # Check subscription
POST /api/subscriptions/presence/subscribe                  # Subscribe to presence
POST /api/subscriptions/presence/unsubscribe                # Unsubscribe from presence
GET  /api/subscriptions/websocket/{userId}/sessions         # Get WebSocket sessions
GET  /api/subscriptions/websocket/session/{sessionId}       # Get session details
POST /api/subscriptions/cleanup                             # Manual cleanup
```

## Implementation Details

### 1. Session Management

#### Connection Registration
```java
webSocketConnectionService.registerConnection(userId, sessionId, deviceInfo);
```

#### Session Refresh (Heartbeat)
```java
webSocketConnectionService.refreshSession(userId, sessionId);
presenceService.refreshUserSession(userId, sessionId);
```

#### Session Cleanup
```java
webSocketConnectionService.cleanupExpiredSessions();
```

### 2. Presence Tracking

#### Set User Online
```java
presenceService.setUserOnline(userId, deviceInfo, sessionId);
```

#### Set User Offline
```java
presenceService.setUserOffline(userId, sessionId);
```

#### Check Online Status
```java
boolean isOnline = presenceService.isUserOnline(userId);
```

### 3. Subscription Management

#### Subscribe to Presence
```java
presenceSubscriptionService.subscribeToPresence(subscriberId, targetUserId);
```

#### Get Subscriptions
```java
Set<UUID> subscriptions = presenceSubscriptionService.getSubscriptions(userId);
```

## Configuration and Setup

### 1. Redis Configuration
- Enable keyspace notifications: `CONFIG SET notify-keyspace-events KEA`
- Configure Redis connection in `application.properties`

### 2. Scheduled Tasks
- **Subscription cleanup**: Every 30 minutes
- **WebSocket session cleanup**: Every 15 minutes
- **Presence session cleanup**: Every 10 minutes
- **Health check**: Every hour

### 3. Key Expiration Handling
- **RedisKeyExpirationListener**: Automatic handling of expired keys
- **Session TTL**: 60 seconds for all sessions
- **Subscription TTL**: 1 hour for subscriptions

## Data Flow

### 1. User Connection Flow
1. User connects to WebSocket
2. WebSocket interceptor authenticates user
3. Connection registered in WebSocketConnectionService
4. User presence set to online in PresenceService
5. Presence event published to Kafka
6. Subscribers receive presence update

### 2. Heartbeat Flow
1. Client sends heartbeat via `/app/heartbeat`
2. WebSocketConnectionService refreshes session TTL
3. PresenceService refreshes presence session TTL
4. Device information updated if provided

### 3. Disconnection Flow
1. WebSocket session expires (60s TTL)
2. RedisKeyExpirationListener detects expiration
3. Checks if user has other active sessions
4. If no active sessions, sets user offline
5. Offline event published to Kafka
6. Subscribers receive offline notification

## Monitoring and Debugging

### 1. Key Metrics
- Active WebSocket connections per user
- Presence session count
- Subscription count
- Heartbeat frequency

### 2. Logging
- Session creation/expiration events
- Presence state changes
- Subscription changes
- Cleanup operations

### 3. Health Checks
- Redis connectivity
- Session consistency
- Subscription consistency
- Kafka publishing status

## Error Handling

### 1. Session Expiration
- Automatic cleanup of expired sessions
- Graceful handling of stale connections
- Debounce mechanism for offline events

### 2. Redis Connection Issues
- Retry mechanisms for Redis operations
- Fallback strategies for critical operations
- Circuit breaker patterns

### 3. Kafka Publishing
- Retry logic for failed events
- Dead letter queues for failed messages
- Event ordering guarantees

## Performance Considerations

### 1. Redis Optimization
- Use Redis pipelines for bulk operations
- Optimize key patterns for efficient queries
- Monitor Redis memory usage

### 2. Session Management
- Efficient session lookup algorithms
- Batch cleanup operations
- TTL-based automatic cleanup

### 3. Subscription Scaling
- Efficient subscription storage
- Batch subscription operations
- Subscription deduplication

## Testing

### 1. Unit Tests
- Service layer testing
- Redis operations testing
- Session management testing

### 2. Integration Tests
- WebSocket connection testing
- Presence tracking testing
- Subscription management testing

### 3. Load Testing
- Concurrent connection handling
- Session expiration under load
- Subscription performance testing

## Deployment Considerations

### 1. Redis Configuration
- High availability Redis setup
- Redis cluster configuration
- Backup and recovery strategies

### 2. Monitoring
- Application metrics
- Redis metrics
- WebSocket connection metrics

### 3. Scaling
- Horizontal scaling strategies
- Load balancing considerations
- Session affinity management

## Troubleshooting

### Common Issues

1. **Sessions not expiring properly**
   - Check Redis keyspace notifications
   - Verify TTL configuration
   - Monitor RedisKeyExpirationListener

2. **Presence not updating**
   - Check WebSocket connection status
   - Verify session refresh mechanisms
   - Monitor Kafka event publishing

3. **Subscription not working**
   - Check subscription TTL
   - Verify subscription storage
   - Monitor subscription refresh

### Debug Tools

1. **Redis CLI Commands**
   ```bash
   # Check online users
   SMEMBERS presence:online
   
   # Check user sessions
   KEYS presence:user:*:online:*
   
   # Check subscriptions
   SMEMBERS presence:subscription:{userId}
   ```

2. **API Endpoints for Debugging**
   ```
   GET /api/subscriptions/websocket/{userId}/sessions
   GET /api/subscriptions/presence/{userId}
   POST /api/subscriptions/cleanup
   ```

## Future Enhancements

1. **Geographic Presence**
   - Location-based presence tracking
   - Regional presence servers

2. **Advanced Subscription Features**
   - Conditional subscriptions
   - Subscription filters
   - Subscription analytics

3. **Performance Optimization**
   - Connection pooling
   - Caching strategies
   - Batch operations

4. **Security Enhancements**
   - Subscription permissions
   - Rate limiting
   - Connection validation
