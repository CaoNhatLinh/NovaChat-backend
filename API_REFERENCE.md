# NovaChat Backend - Complete API Reference

## 📋 Table of Contents

1. [REST API Endpoints](#rest-api-endpoints)
2. [WebSocket Endpoints](#websocket-endpoints)
3. [Authentication](#authentication)
4. [Error Handling](#error-handling)
5. [Rate Limiting](#rate-limiting)

---

## 🌐 REST API Endpoints

### Authentication

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "display_name": "John Doe"
}

Response: 201 Created
{
  "user_id": "uuid",
  "username": "john_doe",
  "email": "john@example.com",
  "display_name": "John Doe",
  "token": "jwt_token_here"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123!"
}

Response: 200 OK
{
  "user_id": "uuid",
  "username": "john_doe",
  "token": "jwt_token_here",
  "refresh_token": "refresh_token_here"
}
```

---

### Conversations

#### Get User Conversations
```http
GET /api/conversations/my
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "conversationId": "uuid",
    "name": "Team Discussion",
    "type": "GROUP",
    "lastMessage": {
      "content": "Hello everyone!",
      "createdAt": "2025-10-02T10:30:00Z",
      "sender": {...}
    },
    "unreadCount": 5,
    "memberIds": ["uuid1", "uuid2"],
    "createdAt": "2025-10-01T00:00:00Z"
  }
]
```

#### Search Conversations (Elasticsearch)
```http
GET /api/conversations/search?name=project&type=GROUP&page=0&size=20
Authorization: Bearer {token}

Response: 200 OK
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "hasContent": true
}
```

#### Create Conversation
```http
POST /api/conversations
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Project Team",
  "type": "GROUP",
  "memberIds": ["uuid1", "uuid2", "uuid3"]
}

Response: 201 Created
{
  "conversationId": "uuid",
  "name": "Project Team",
  "type": "GROUP",
  "memberIds": ["uuid1", "uuid2", "uuid3"],
  "createdAt": "2025-10-02T11:00:00Z"
}
```

#### Find DM Conversation
```http
GET /api/conversations/dm?userId1={uuid1}&userId2={uuid2}
Authorization: Bearer {token}

Response: 200 OK
{
  "conversationId": "uuid",
  "type": "DM",
  "memberIds": ["uuid1", "uuid2"]
}

Response: 404 Not Found (if no conversation exists)
```

#### Delete Conversation (Soft Delete)
```http
DELETE /api/conversations/{conversationId}
Authorization: Bearer {token}

Response: 200 OK
{
  "message": "Conversation deleted successfully"
}
```

#### Restore Conversation
```http
PUT /api/conversations/{conversationId}/restore
Authorization: Bearer {token}

Response: 200 OK
{
  "message": "Conversation restored successfully"
}
```

---

### Messages

#### Get Messages
```http
GET /api/messages/{conversationId}?page=0&size=50
Authorization: Bearer {token}

Response: 200 OK
{
  "content": [
    {
      "messageId": "uuid",
      "conversationId": "uuid",
      "content": "Hello!",
      "sender": {
        "user_id": "uuid",
        "username": "john_doe",
        "display_name": "John Doe",
        "avatar_url": "https://..."
      },
      "messageType": "TEXT",
      "createdAt": "2025-10-02T10:30:00Z",
      "isDeleted": false,
      "reactions": [
        {
          "emoji": "👍",
          "userId": "uuid",
          "username": "jane_doe"
        }
      ],
      "mentions": ["uuid1", "uuid2"],
      "replyTo": {
        "messageId": "uuid",
        "content": "Original message"
      }
    }
  ],
  "totalPages": 10,
  "hasContent": true
}
```

#### Send Message (via REST - use WebSocket for real-time)
```http
POST /api/messages/{conversationId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "Hello everyone!",
  "type": "TEXT",
  "mentionedUserIds": ["uuid1", "uuid2"],
  "replyTo": "original-message-uuid"
}

Response: 201 Created
{
  "messageId": "uuid",
  "content": "Hello everyone!",
  "createdAt": "2025-10-02T11:00:00Z"
}
```

#### Delete Message
```http
DELETE /api/messages/{conversationId}/{messageId}
Authorization: Bearer {token}

Response: 200 OK
{
  "message": "Message deleted successfully"
}
```

---

### Friends

#### Get Friends List
```http
GET /api/friends
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "user_id": "uuid",
    "username": "jane_doe",
    "display_name": "Jane Doe",
    "avatar_url": "https://...",
    "isOnline": true,
    "lastSeen": "2025-10-02T10:30:00Z"
  }
]
```

#### Send Friend Request
```http
POST /api/friends/request/{userId}
Authorization: Bearer {token}

Response: 200 OK
{
  "message": "Friend request sent successfully"
}
```

#### Accept Friend Request
```http
PUT /api/friends/accept/{userId}
Authorization: Bearer {token}

Response: 200 OK
{
  "message": "Friend request accepted"
}
```

#### Reject Friend Request
```http
PUT /api/friends/reject/{userId}
Authorization: Bearer {token}

Response: 200 OK
{
  "message": "Friend request rejected"
}
```

#### Remove Friend
```http
DELETE /api/friends/{userId}
Authorization: Bearer {token}

Response: 200 OK
{
  "message": "Friend removed successfully"
}
```

---

### Notifications

#### Get Notifications
```http
GET /api/notifications?page=0&size=20
Authorization: Bearer {token}

Response: 200 OK
{
  "content": [
    {
      "notificationId": "uuid",
      "title": "New Message",
      "body": "John sent you a message",
      "type": "MESSAGE",
      "isRead": false,
      "metadata": {
        "conversationId": "uuid",
        "messageId": "uuid"
      },
      "createdAt": "2025-10-02T10:30:00Z"
    }
  ],
  "hasContent": true
}
```

#### Get Unread Count
```http
GET /api/notifications/unread/count
Authorization: Bearer {token}

Response: 200 OK
{
  "count": 5
}
```

#### Mark as Read
```http
PUT /api/notifications/{notificationId}/read
Authorization: Bearer {token}

Response: 200 OK
```

#### Mark All as Read
```http
PUT /api/notifications/read-all
Authorization: Bearer {token}

Response: 200 OK
```

---

### File Upload

#### Upload File
```http
POST /api/files/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [binary]
conversationId: uuid

Response: 200 OK
{
  "url": "https://cloudinary.com/...",
  "fileName": "image.jpg",
  "contentType": "image/jpeg",
  "fileSize": 1024000,
  "resourceType": "image",
  "publicId": "chat/abc123",
  "thumbnailUrl": "https://...",
  "mediumUrl": "https://..."
}
```

---

## 🔌 WebSocket Endpoints

### Connection

Connect to: `ws://localhost:8084/ws`

**Authentication**: Include JWT token in STOMP headers:
```javascript
const headers = {
    'Authorization': 'Bearer ' + token
};

stompClient.connect(headers, onConnected, onError);
```

---

### Subscribe Channels (Client listens)

#### Personal Messages
```javascript
stompClient.subscribe('/user/queue/messages', function(message) {
    const messageData = JSON.parse(message.body);
    // Handle new message
});
```

#### Message Echo (Immediate feedback)
```javascript
stompClient.subscribe('/user/queue/message-echo', function(message) {
    const echoData = JSON.parse(message.body);
    // Display message immediately while processing
});
```

#### Notifications
```javascript
stompClient.subscribe('/user/queue/notifications', function(notification) {
    const notifData = JSON.parse(notification.body);
    // Show notification
});
```

#### Online Status Updates
```javascript
stompClient.subscribe('/user/queue/online-status', function(statusUpdate) {
    const status = JSON.parse(statusUpdate.body);
    // Update online/offline indicators
});
```

#### Conversation Updates
```javascript
const conversationId = 'uuid';
stompClient.subscribe(`/topic/conversation/${conversationId}`, function(update) {
    const data = JSON.parse(update.body);
    // Handle conversation update
});
```

#### Typing Indicators
```javascript
const conversationId = 'uuid';
stompClient.subscribe(`/topic/conversation/${conversationId}/typing`, function(typingEvent) {
    const typing = JSON.parse(typingEvent.body);
    // Show/hide typing indicator
});
```

#### Error Messages
```javascript
stompClient.subscribe('/user/queue/errors', function(error) {
    const errorData = JSON.parse(error.body);
    // Display error message
});
```

---

### Send Messages (Client sends)

#### Send Text Message
```javascript
stompClient.send('/app/message.send', {}, JSON.stringify({
    payload: {
        conversationId: 'uuid',
        content: 'Hello!',
        type: 'TEXT',
        mentions: ['uuid1', 'uuid2'],
        replyTo: 'original-message-uuid'
    }
}));
```

#### Send File/Image/Video Message
```javascript
// First upload file via REST API to get URL
// Then send via WebSocket
stompClient.send('/app/message.file', {}, JSON.stringify({
    payload: {
        conversationId: 'uuid',
        content: 'Check this out!',
        type: 'IMAGE', // or 'VIDEO', 'AUDIO', 'FILE'
        attachments: [
            {
                url: 'https://cloudinary.com/...',
                fileName: 'photo.jpg',
                contentType: 'image/jpeg',
                fileSize: 1024000,
                resourceType: 'image',
                publicId: 'chat/abc123',
                thumbnailUrl: 'https://...',
                mediumUrl: 'https://...'
            }
        ]
    }
}));
```

#### Send Typing Indicator
```javascript
// Start typing
stompClient.send('/app/typing', {}, JSON.stringify({
    conversationId: 'uuid',
    typing: true
}));

// Stop typing (optional - Redis TTL auto-expires after 2s)
stompClient.send('/app/typing', {}, JSON.stringify({
    conversationId: 'uuid',
    typing: false
}));
```

#### Request Online Status
```javascript
stompClient.send('/app/request-online-status', {}, JSON.stringify({
    userIds: ['uuid1', 'uuid2', 'uuid3']
}));
```

#### Send Heartbeat
```javascript
// Send every 30 seconds to keep session alive
stompClient.send('/app/heartbeat', {}, JSON.stringify({
    deviceInfo: navigator.userAgent,
    timestamp: Date.now()
}));
```

#### Mark Notification as Read
```javascript
stompClient.send('/app/notification.read', {}, JSON.stringify({
    notificationId: 'uuid'
}));
```

#### Mark All Notifications as Read
```javascript
stompClient.send('/app/notifications.read-all', {}, '{}');
```

---

## 🔐 Authentication

### JWT Token

All REST API requests require JWT token in header:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### WebSocket Authentication

Include token in STOMP connect headers:
```javascript
const headers = {
    'Authorization': 'Bearer ' + token
};
stompClient.connect(headers, onConnected, onError);
```

### Token Expiration

- Access Token: 24 hours
- Refresh Token: 7 days

### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh_token_here"
}

Response: 200 OK
{
  "token": "new_jwt_token",
  "refreshToken": "new_refresh_token"
}
```

---

## ⚠️ Error Handling

### Error Response Format

```json
{
  "type": "ERROR",
  "message": "Detailed error message",
  "timestamp": "2025-10-02T10:30:00Z",
  "status": 400,
  "path": "/api/messages"
}
```

### HTTP Status Codes

- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - No permission
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error

### Common Error Types

```json
// Authentication Error
{
  "status": 401,
  "message": "Invalid or expired token"
}

// Validation Error
{
  "status": 400,
  "message": "Content cannot be empty"
}

// Not Found
{
  "status": 404,
  "message": "Conversation not found"
}

// Permission Denied
{
  "status": 403,
  "message": "You are not a member of this conversation"
}
```

---

## 🚦 Rate Limiting

### Current Limits

- **REST API**: 100 requests per minute per user
- **WebSocket Messages**: 50 messages per minute per user
- **File Uploads**: 10 files per minute per user
- **Search**: 20 queries per minute per user

### Rate Limit Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1696248000
```

### Rate Limit Exceeded Response

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{
  "status": 429,
  "message": "Rate limit exceeded. Please try again later.",
  "retryAfter": 60
}
```

---

## 📊 Pagination

### Request Parameters

- `page`: Page number (0-indexed, default: 0)
- `size`: Items per page (default: 20, max: 100)
- `sort`: Sort criteria (e.g., `createdAt,desc`)

### Response Format

```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0,
  "hasContent": true,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## 🔍 Search Filters

### Conversation Search

```http
GET /api/conversations/search?name=project&type=GROUP&page=0&size=20
```

Parameters:
- `name`: Search by conversation name (partial match)
- `type`: Filter by type (`GROUP` or `DM`)
- `page`: Page number
- `size`: Page size

---

## 📝 Notes

### Message Types

- `TEXT` - Plain text message
- `IMAGE` - Image attachment
- `VIDEO` - Video attachment
- `AUDIO` - Audio/voice message
- `FILE` - Document/file attachment

### Notification Types

- `MESSAGE` - New message
- `REACTION` - Message reaction
- `MENTION` - User mention
- `FRIEND_REQUEST` - Friend request
- `CONVERSATION_INVITE` - Group invite
- `POLL` - New poll
- `PIN_MESSAGE` - Message pinned

### Conversation Types

- `DM` - Direct message (1-on-1)
- `GROUP` - Group conversation (2+ members)

---

**Last Updated**: October 2, 2025  
**API Version**: v1.0.0
