# Chat Service - Cấu trúc Service Đã Tái Cấu Trúc

## 📁 Cấu trúc Package Mới

### 1. Service Package Structure

```
src/main/java/com/chatapp/chat_service/service/
├── message/
│   ├── MessageValidationService.java      # Validation cho message operations
│   └── MessageService.java                # Core message operations (đã cleaned up)
├── presence/
│   ├── PresenceService.java               # Session-based presence management
│   └── OnlineStatusService.java           # Simple online status management
├── typing/
│   └── TypingIndicatorService.java        # Typing indicators với Redis TTL
├── websocket/
│   └── WebSocketConnectionService.java    # WebSocket connection management
├── AuthService.java                       # Authentication services
├── UserService.java                       # User management
├── ConversationService.java               # Conversation management
├── FriendService.java                     # Friend management
├── NotificationService.java               # Notification services
└── ...
```

## 🔧 Các Service Đã Tái Cấu Trúc

### 1. **MessageService** (Cleaned Up)
- **Mục đích**: Chỉ xử lý message operations
- **Removed**: Online status, typing indicators, WebSocket connection management
- **Dependencies**: MessageValidationService, ConversationElasticsearchService
- **Methods**: 
  - `sendMessage()`
  - `getLatestMessages()`
  - `getOlderMessages()`
  - `getConversationMessages()`

### 2. **OnlineStatusService** (New)
- **Mục đích**: Quản lý online status đơn giản
- **Features**: 
  - Simple online/offline tracking
  - Redis-based storage
  - Kafka event publishing
- **Methods**:
  - `setUserOnline(userId)`
  - `setUserOffline(userId)`
  - `isUserOnline(userId)`
  - `getOnlineUsers()`

### 3. **PresenceService** (Enhanced)
- **Mục đích**: Session-based presence management
- **Features**:
  - Multi-device support
  - Session tracking with TTL
  - Automatic timeout management
  - Presence watchers
- **Methods**:
  - `setUserOnline(userId, device, sessionId)`
  - `refreshUserSession(userId, sessionId)`
  - `getUserActiveSessions(userId)`
  - `getPresenceResponse(userId)`

### 4. **TypingIndicatorService** (New)
- **Mục đích**: Quản lý typing indicators
- **Features**:
  - Redis với TTL tự động
  - Conversation-based typing
  - Cleanup utilities
- **Methods**:
  - `startTyping(conversationId, userId)`
  - `stopTyping(conversationId, userId)`
  - `getTypingUsers(conversationId)`
  - `isUserTyping(conversationId, userId)`

### 5. **WebSocketConnectionService** (New)
- **Mục đích**: Quản lý WebSocket connections
- **Features**:
  - Connection counting
  - TTL-based cleanup
  - Multi-connection support
- **Methods**:
  - `registerConnection(userId)`
  - `unregisterConnection(userId)`
  - `hasActiveConnection(userId)`
  - `getActiveConnectionCount(userId)`

### 6. **MessageValidationService** (New)
- **Mục đích**: Validation cho message operations
- **Features**:
  - Conversation membership validation
  - Message permission checking
- **Methods**:
  - `validateConversationMembership(conversationId, userId)`
  - `validateMessagePermission(conversationId, userId)`

## 🔄 Migration Guide

### Controllers và Services đã được cập nhật:

1. **WebSocketChatController**
   - Sử dụng `TypingIndicatorService` thay vì `MessageService` cho typing
   - Import từ `service.presence.PresenceService`

2. **WebSocketConnectHandler**
   - Sử dụng `WebSocketConnectionService` và `OnlineStatusService`
   - Loại bỏ dependency vào `MessageService`

3. **WebSocketDisconnectHandler**
   - Sử dụng `WebSocketConnectionService` và `OnlineStatusService`
   - Loại bỏ dependency vào `MessageService`

4. **KafkaMessageConsumer**
   - Sử dụng `OnlineStatusService` cho online status management
   - Loại bỏ dependency vào `MessageService` cho online status

5. **UserService**
   - Sử dụng `OnlineStatusService` cho `isUserOnline()` method

## 🎯 Lợi ích của Cấu trúc Mới

### 1. **Separation of Concerns**
- Mỗi service có một trách nhiệm rõ ràng
- Dễ dàng maintain và extend

### 2. **Reusability**
- Services có thể được sử dụng độc lập
- Tránh circular dependencies

### 3. **Testability**
- Dễ dàng mock và test từng service
- Unit tests rõ ràng hơn

### 4. **Scalability**
- Có thể tách thành microservices dễ dàng
- Load balancing theo domain

### 5. **Maintainability**
- Code dễ đọc và hiểu
- Giảm complexity của các service lớn

## 🚀 Next Steps

1. **Testing**: Viết unit tests cho các service mới
2. **Documentation**: Cập nhật API documentation
3. **Monitoring**: Thêm metrics cho các service
4. **Performance**: Optimize Redis queries và caching
5. **Security**: Validate permissions trong từng service

## 🔍 Debugging Tips

- **Online Status Issues**: Check `OnlineStatusService` logs
- **Typing Issues**: Check `TypingIndicatorService` Redis keys
- **Connection Issues**: Check `WebSocketConnectionService` connection counts
- **Presence Issues**: Check `PresenceService` session tracking

## 📊 Redis Key Patterns

```
# Online Status
user:online -> Set của user IDs

# WebSocket Connections  
user:ws:connections:{userId} -> Connection count

# Typing Indicators
conversation:typing:{conversationId}:{userId} -> "typing" with TTL

# Presence Sessions
presence:user:{userId}:online:{sessionId} -> "true" with TTL
presence:user:{userId}:last_active -> timestamp
presence:user:{userId}:watchers -> Set of watcher IDs
```

Cấu trúc mới này giúp code dễ maintain, test và scale hơn đáng kể so với cấu trúc cũ!
