# WebSocket Connection and Presence System - Usage Examples

## 1. Frontend Integration Examples

### JavaScript WebSocket Client Setup

```javascript
// WebSocket connection với authentication
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// Authentication header
const headers = {
    'Authorization': `Bearer ${token}`
};

stompClient.connect(headers, function (frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to presence updates
    subscribeToPresenceUpdates();
    
    // Start heartbeat
    startHeartbeat();
    
    // Subscribe to typing indicators
    subscribeToTypingIndicators();
    
}, function (error) {
    console.log('WebSocket connection error: ' + error);
});
```

### Heartbeat Implementation

```javascript
// Send heartbeat every 30 seconds to maintain session
function startHeartbeat() {
    setInterval(() => {
        if (stompClient && stompClient.connected) {
            stompClient.send('/app/heartbeat', {}, JSON.stringify({
                deviceInfo: navigator.userAgent,
                timestamp: Date.now()
            }));
        }
    }, 30000); // 30 seconds
}
```

### Presence Subscription

```javascript
// Subscribe to presence updates for friends
function subscribeToPresenceUpdates() {
    // Get friends list
    const friendIds = ['user1-uuid', 'user2-uuid', 'user3-uuid'];
    
    // Subscribe to presence updates
    stompClient.send('/app/presence.subscribe', {}, JSON.stringify({
        userIds: friendIds
    }));
    
    // Listen for presence updates
    stompClient.subscribe('/user/queue/presence', function (message) {
        const presenceUpdate = JSON.parse(message.body);
        handlePresenceUpdate(presenceUpdate);
    });
}

function handlePresenceUpdate(presenceUpdate) {
    console.log('Presence update:', presenceUpdate);
    
    // Update UI to show online/offline status
    const userElement = document.getElementById(`user-${presenceUpdate.userId}`);
    if (userElement) {
        userElement.classList.toggle('online', presenceUpdate.online);
        userElement.classList.toggle('offline', !presenceUpdate.online);
    }
}
```

## 2. REST API Usage Examples

### Subscribe to Presence Updates

```bash
# Subscribe to multiple users' presence
curl -X POST http://localhost:8080/api/subscriptions/presence/subscribe \
  -H "Content-Type: application/json" \
  -d '{
    "subscriberId": "12345678-1234-1234-1234-123456789012",
    "userIds": [
      "87654321-4321-4321-4321-210987654321",
      "11111111-2222-3333-4444-555555555555"
    ]
  }'
```

### Get User's Subscriptions

```bash
# Get all subscriptions for a user
curl -X GET http://localhost:8080/api/subscriptions/presence/12345678-1234-1234-1234-123456789012
```

### Get WebSocket Sessions

```bash
# Get active WebSocket sessions for a user
curl -X GET http://localhost:8080/api/subscriptions/websocket/12345678-1234-1234-1234-123456789012/sessions
```

### Check Subscription Status

```bash
# Check if user A is subscribed to user B's presence
curl -X GET http://localhost:8080/api/subscriptions/presence/12345678-1234-1234-1234-123456789012/subscribed-to/87654321-4321-4321-4321-210987654321
```

## 3. React Component Examples

### Online Status Indicator Component

```jsx
import React, { useState, useEffect } from 'react';
import { useWebSocket } from './hooks/useWebSocket';

const OnlineStatusIndicator = ({ userId }) => {
    const [isOnline, setIsOnline] = useState(false);
    const { stompClient } = useWebSocket();

    useEffect(() => {
        if (stompClient && stompClient.connected) {
            // Subscribe to this user's presence
            stompClient.send('/app/presence.subscribe', {}, JSON.stringify({
                userIds: [userId]
            }));

            // Listen for presence updates
            const subscription = stompClient.subscribe('/user/queue/presence', (message) => {
                const presenceUpdate = JSON.parse(message.body);
                if (presenceUpdate.userId === userId) {
                    setIsOnline(presenceUpdate.online);
                }
            });

            return () => {
                subscription.unsubscribe();
            };
        }
    }, [stompClient, userId]);

    return (
        <div className={`online-indicator ${isOnline ? 'online' : 'offline'}`}>
            <span className="status-dot"></span>
            <span className="status-text">{isOnline ? 'Online' : 'Offline'}</span>
        </div>
    );
};
```

### Friends List with Presence

```jsx
import React, { useState, useEffect } from 'react';
import { useWebSocket } from './hooks/useWebSocket';

const FriendsList = ({ friends }) => {
    const [friendsPresence, setFriendsPresence] = useState({});
    const { stompClient } = useWebSocket();

    useEffect(() => {
        if (stompClient && stompClient.connected && friends.length > 0) {
            // Subscribe to all friends' presence
            const friendIds = friends.map(friend => friend.id);
            stompClient.send('/app/presence.subscribe', {}, JSON.stringify({
                userIds: friendIds
            }));

            // Listen for presence updates
            const subscription = stompClient.subscribe('/user/queue/presence', (message) => {
                const presenceUpdate = JSON.parse(message.body);
                setFriendsPresence(prev => ({
                    ...prev,
                    [presenceUpdate.userId]: presenceUpdate.online
                }));
            });

            return () => {
                subscription.unsubscribe();
            };
        }
    }, [stompClient, friends]);

    return (
        <div className="friends-list">
            {friends.map(friend => (
                <div key={friend.id} className="friend-item">
                    <div className="friend-avatar">
                        <img src={friend.avatar} alt={friend.name} />
                        <span className={`presence-dot ${friendsPresence[friend.id] ? 'online' : 'offline'}`}></span>
                    </div>
                    <div className="friend-info">
                        <h4>{friend.name}</h4>
                        <p className="presence-status">
                            {friendsPresence[friend.id] ? 'Online' : 'Offline'}
                        </p>
                    </div>
                </div>
            ))}
        </div>
    );
};
```

## 4. Backend Service Usage Examples

### Using WebSocketConnectionService

```java
@Service
public class ChatService {
    
    @Autowired
    private WebSocketConnectionService webSocketConnectionService;
    
    // Register new connection
    public void handleNewConnection(UUID userId, String sessionId, String deviceInfo) {
        webSocketConnectionService.registerConnection(userId, sessionId, deviceInfo);
        
        // Check if user has multiple devices
        Set<String> activeSessions = webSocketConnectionService.getActiveSessions(userId);
        if (activeSessions.size() > 1) {
            log.info("User {} has multiple devices connected: {}", userId, activeSessions.size());
        }
    }
    
    // Handle disconnection
    public void handleDisconnection(UUID userId, String sessionId) {
        webSocketConnectionService.removeConnection(userId, sessionId);
        
        // Check if user still has active connections
        boolean hasActiveConnections = webSocketConnectionService.hasActiveConnection(userId);
        if (!hasActiveConnections) {
            log.info("User {} has no more active connections", userId);
        }
    }
}
```

### Using PresenceService

```java
@Service
public class UserPresenceService {
    
    @Autowired
    private PresenceService presenceService;
    
    // Set user online when they connect
    public void userConnected(UUID userId, String deviceInfo, String sessionId) {
        presenceService.setUserOnline(userId, deviceInfo, sessionId);
        
        // Add presence watchers (friends who should be notified)
        Set<UUID> friends = getFriendsForUser(userId);
        for (UUID friendId : friends) {
            presenceService.addPresenceWatcher(userId, friendId);
        }
    }
    
    // Check bulk presence status
    public Map<UUID, Boolean> checkBulkPresence(Set<UUID> userIds) {
        Map<UUID, Boolean> presenceMap = new HashMap<>();
        
        for (UUID userId : userIds) {
            boolean isOnline = presenceService.isUserOnline(userId);
            presenceMap.put(userId, isOnline);
        }
        
        return presenceMap;
    }
}
```

### Using PresenceSubscriptionService

```java
@Service
public class SubscriptionService {
    
    @Autowired
    private PresenceSubscriptionService presenceSubscriptionService;
    
    // Subscribe user to friends' presence
    public void subscribeToFriendsPresence(UUID userId) {
        Set<UUID> friendIds = getFriendsForUser(userId);
        
        for (UUID friendId : friendIds) {
            presenceSubscriptionService.subscribeToPresence(userId, friendId);
        }
        
        log.info("User {} subscribed to {} friends' presence", userId, friendIds.size());
    }
    
    // Get mutual subscriptions
    public Set<UUID> getMutualSubscriptions(UUID userId1, UUID userId2) {
        Set<UUID> user1Subscriptions = presenceSubscriptionService.getSubscriptions(userId1);
        Set<UUID> user2Subscriptions = presenceSubscriptionService.getSubscriptions(userId2);
        
        Set<UUID> mutual = new HashSet<>(user1Subscriptions);
        mutual.retainAll(user2Subscriptions);
        
        return mutual;
    }
}
```

## 5. Testing Examples

### WebSocket Integration Test

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class WebSocketPresenceIntegrationTest {
    
    @Autowired
    private WebSocketConnectionService webSocketConnectionService;
    
    @Autowired
    private PresenceService presenceService;
    
    @Test
    public void testUserPresenceFlow() {
        UUID userId = UUID.randomUUID();
        String sessionId = "test-session-" + System.currentTimeMillis();
        String deviceInfo = "Test Device";
        
        // Test connection registration
        webSocketConnectionService.registerConnection(userId, sessionId, deviceInfo);
        assertTrue(webSocketConnectionService.hasActiveConnection(userId));
        
        // Test presence online
        presenceService.setUserOnline(userId, deviceInfo, sessionId);
        assertTrue(presenceService.isUserOnline(userId));
        
        // Test session refresh
        webSocketConnectionService.refreshSession(userId, sessionId);
        
        // Test disconnection
        webSocketConnectionService.removeConnection(userId, sessionId);
        assertFalse(webSocketConnectionService.hasActiveConnection(userId));
    }
}
```

### Subscription Service Test

```java
@SpringBootTest
public class SubscriptionServiceTest {
    
    @Autowired
    private PresenceSubscriptionService presenceSubscriptionService;
    
    @Test
    public void testSubscriptionFlow() {
        UUID subscriberId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        
        // Test subscription
        presenceSubscriptionService.subscribeToPresence(subscriberId, targetUserId);
        assertTrue(presenceSubscriptionService.isSubscribed(subscriberId, targetUserId));
        
        // Test getting subscriptions
        Set<UUID> subscriptions = presenceSubscriptionService.getSubscriptions(subscriberId);
        assertTrue(subscriptions.contains(targetUserId));
        
        // Test getting subscribers
        Set<UUID> subscribers = presenceSubscriptionService.getSubscribers(targetUserId);
        assertTrue(subscribers.contains(subscriberId));
        
        // Test unsubscription
        presenceSubscriptionService.unsubscribeFromPresence(subscriberId, targetUserId);
        assertFalse(presenceSubscriptionService.isSubscribed(subscriberId, targetUserId));
    }
}
```

## 6. Performance Monitoring Examples

### Redis Monitoring

```bash
# Monitor Redis keys
redis-cli --scan --pattern "ws:*" | head -10
redis-cli --scan --pattern "presence:*" | head -10

# Check memory usage
redis-cli INFO memory

# Monitor key expiration
redis-cli MONITOR | grep "expired"
```

### Application Metrics

```java
@Component
public class WebSocketMetrics {
    
    private final MeterRegistry meterRegistry;
    private final WebSocketConnectionService webSocketConnectionService;
    
    public WebSocketMetrics(MeterRegistry meterRegistry, 
                           WebSocketConnectionService webSocketConnectionService) {
        this.meterRegistry = meterRegistry;
        this.webSocketConnectionService = webSocketConnectionService;
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void recordMetrics() {
        // Record active connection count
        long totalConnections = getTotalActiveConnections();
        meterRegistry.gauge("websocket.connections.active", totalConnections);
        
        // Record presence metrics
        long onlineUsers = getOnlineUserCount();
        meterRegistry.gauge("presence.users.online", onlineUsers);
    }
    
    private long getTotalActiveConnections() {
        // Implementation to count total active connections
        return 0; // Placeholder
    }
    
    private long getOnlineUserCount() {
        // Implementation to count online users
        return 0; // Placeholder
    }
}
```

## 7. Error Handling Examples

### Connection Error Handling

```javascript
// WebSocket connection with retry logic
function connectWebSocket() {
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);
    
    stompClient.connect(headers, 
        function (frame) {
            console.log('Connected: ' + frame);
            resetReconnectAttempts();
            startHeartbeat();
        },
        function (error) {
            console.log('Connection error: ' + error);
            scheduleReconnect();
        }
    );
}

let reconnectAttempts = 0;
const maxReconnectAttempts = 5;

function scheduleReconnect() {
    if (reconnectAttempts < maxReconnectAttempts) {
        const delay = Math.pow(2, reconnectAttempts) * 1000; // Exponential backoff
        setTimeout(() => {
            reconnectAttempts++;
            connectWebSocket();
        }, delay);
    }
}

function resetReconnectAttempts() {
    reconnectAttempts = 0;
}
```

### Backend Error Handling

```java
@Component
public class WebSocketErrorHandler {
    
    @EventListener
    public void handleConnectionError(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        UUID userId = getUserIdFromSession(sessionId);
        
        if (userId != null) {
            try {
                webSocketConnectionService.removeConnection(userId, sessionId);
                log.info("Cleaned up connection for user {} session {}", userId, sessionId);
            } catch (Exception e) {
                log.error("Error cleaning up connection for user {} session {}", userId, sessionId, e);
            }
        }
    }
    
    @EventListener
    public void handlePresenceError(PresenceUpdateFailureEvent event) {
        UUID userId = event.getUserId();
        String operation = event.getOperation();
        
        log.error("Presence update failed for user {} operation {}", userId, operation);
        
        // Implement retry logic or fallback mechanism
        schedulePresenceRetry(userId, operation);
    }
}
```

These examples demonstrate comprehensive usage of the WebSocket connection and presence management system across different layers of the application, from frontend JavaScript to backend Java services, including testing and monitoring scenarios.
