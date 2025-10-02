# NovaChat Backend

A real-time chat application backend built with Spring Boot, featuring WebSocket communication, Elasticsearch search, and comprehensive notification system.

## 🚀 Features

### ✅ Implemented Features

- **Real-time Messaging**: WebSocket-based instant messaging with STOMP protocol
- **File Attachments**: Support for images, videos, audio, and documents via Cloudinary
- **Typing Indicators**: Real-time typing status with Redis TTL (2s auto-expiry)
- **User Presence System**: Basic online/offline status tracking (session-based)
- **Friend Management**: Send/accept/reject friend requests and manage friend list
- **Conversation Management**: 
  - DM (Direct Message) conversations
  - Group conversations
  - Soft delete with restore capability
  - Conversation search via Elasticsearch
- **Notification System**: 
  - Real-time notifications via WebSocket
  - Kafka event streaming for scalability
  - Multiple notification types (message, friend request, etc.)
- **Message Features**:
  - Reply to messages
  - Message deletion
  - Read receipts
  - Immediate echo feedback for better UX
- **Authentication**: JWT-based authentication with Spring Security
- **Caching**: Redis caching for optimal performance
- **WebSocket**: STOMP over WebSocket for bidirectional communication

### 🚧 Partially Implemented / In Progress

- **User Presence System**: Basic implementation only (Fan-out presence system not completed)
- **Search**: Conversation search works, but message content search not yet implemented

### ❌ Not Yet Implemented

- **Message Reactions**: Emoji reactions to messages
- **Polls**: Create and vote on polls within conversations
- **Pin Messages**: Pin/Unpin important messages
- **Mentions**: Tag users with @username in messages
- **Audit Logging**: Detailed tracking of user activities
- **Voice/Video Calls**: Real-time audio/video communication
- **Message Search**: Full-text search for message content
- **Advanced Presence**: Fan-out presence system for large-scale deployments

## 🛠️ Tech Stack

- **Java 20** with Spring Boot 3.5.3
- **Apache Cassandra** - Primary database for chat data
- **Redis** - Caching and session management
- **Elasticsearch** - Search engine for conversations and messages
- **Apache Kafka** - Message queue for event streaming
- **WebSocket (STOMP)** - Real-time bidirectional communication
- **Spring Security** - Authentication and authorization
- **Docker & Docker Compose** - Containerization

## 📋 Prerequisites

- Java 20 or higher
- Docker and Docker Compose
- Maven 3.6+

## 🔧 Installation & Setup

### 1. Clone the repository

```bash
git clone https://github.com/CaoNhatLinh/NovaChat-backend.git
cd NovaChat-backend
```

### 2. Start required services with Docker

```bash
# Start Cassandra, Redis, Kafka, Zookeeper
docker-compose up -d

# Optional: Start Elasticsearch (if using search features)
docker-compose -f docker-compose-elasticsearch.yml up -d
```

### 3. Configure application

Update `src/main/resources/application.properties` with your configurations:

```properties
# Cassandra
spring.cassandra.keyspace-name=chat_db
spring.cassandra.contact-points=localhost
spring.cassandra.port=9042

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# Elasticsearch (optional)
spring.elasticsearch.uris=http://localhost:9200
```

### 4. Build and run

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8084`

## 📚 API Documentation

For complete API documentation with detailed examples, see **[API_REFERENCE.md](./API_REFERENCE.md)**

### Quick Reference

**Authentication**
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/refresh` - Refresh JWT token

**Conversations**
- `GET /api/conversations/my` - Get user conversations
- `GET /api/conversations/search` - Search conversations (Elasticsearch)
- `POST /api/conversations` - Create new conversation
- `DELETE /api/conversations/{id}` - Soft delete conversation
- `PUT /api/conversations/{id}/restore` - Restore deleted conversation

**Messages**
- `GET /api/messages/{conversationId}` - Get messages
- `POST /api/messages/{conversationId}` - Send message
- `DELETE /api/messages/{conversationId}/{messageId}` - Delete message

**Notifications**
- `GET /api/notifications` - Get user notifications
- `GET /api/notifications/unread/count` - Get unread count
- `PUT /api/notifications/{id}/read` - Mark as read
- `PUT /api/notifications/read-all` - Mark all as read

**Friends**
- `GET /api/friends` - Get friends list
- `POST /api/friends/request/{userId}` - Send friend request
- `PUT /api/friends/accept/{userId}` - Accept friend request
- `PUT /api/friends/reject/{userId}` - Reject friend request
- `DELETE /api/friends/{userId}` - Remove friend

**File Upload**
- `POST /api/files/upload` - Upload file/image/video

For detailed API documentation with request/response examples, see **[API_REFERENCE.md](./API_REFERENCE.md)**

## 📖 Documentation

### Core Documentation
- **[API Reference](./API_REFERENCE.md)** - Complete REST & WebSocket API documentation
- **[Roadmap](./ROADMAP.md)** - Development plan and future features
- **[Incomplete Features](./INCOMPLETE_FEATURES.md)** - ⚠️ List of unfinished features with implementation guide
- **[Contributing Guide](./CONTRIBUTING.md)** - How to contribute to the project

### Technical Documentation (in `/docs` folder)
- [Complete Notification Service Guide](./docs/COMPLETE_NOTIFICATION_SERVICE_GUIDE.md)
- [WebSocket Presence System Guide](./docs/COMPREHENSIVE_WEBSOCKET_PRESENCE_SYSTEM.md)
- [Elasticsearch Search API](./docs/ELASTICSEARCH_SEARCH_API.md)
- [Frontend Integration Guide](./docs/FRONTEND_INTEGRATION.md)
- [Kafka Configuration](./docs/KAFKA_FIX_DOCUMENTATION.md)
- [Conversation Soft Delete Feature](./docs/CONVERSATION_SOFT_DELETE_FEATURE.md)
- [Service Refactoring Guide](./docs/SERVICE_REFACTORING_GUIDE.md)
- [WebSocket Presence Usage Examples](./docs/WEBSOCKET_PRESENCE_USAGE_EXAMPLES.md)

## 🔌 WebSocket Endpoints

Connect to WebSocket at: `ws://localhost:8084/ws`

### Subscribe Channels (Client listens):
- `/user/queue/messages` - Receive personal messages
- `/user/queue/message-echo` - Receive echo of sent messages (immediate feedback)
- `/user/queue/notifications` - Receive notifications
- `/user/queue/online-status` - Receive presence updates
- `/user/queue/errors` - Receive error messages
- `/topic/conversation/{conversationId}` - Conversation-specific updates
- `/topic/conversation/{conversationId}/typing` - Typing indicators for conversation

### Send Messages (Client sends):
- `/app/message.send` - Send text message
- `/app/message.file` - Send file/image/video/audio message
- `/app/typing` - Send typing indicator (with `typing: true/false`)
- `/app/request-online-status` - Request online status of users
- `/app/heartbeat` - Send heartbeat to keep session alive
- `/app/notification.read` - Mark notification as read
- `/app/notifications.read-all` - Mark all notifications as read

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=MessageServiceTest
```

## 🐳 Docker Services

### Main Services (docker-compose.yml)
- Cassandra: `localhost:9042`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Zookeeper: `localhost:2181`

### Optional Services (docker-compose-elasticsearch.yml)
- Elasticsearch: `localhost:9200`
- Kibana: `localhost:5601`

## 📝 Environment Variables

Create a `.env` file or set environment variables:

```env
JWT_SECRET=your_jwt_secret_key
CASSANDRA_KEYSPACE=chat_db
REDIS_HOST=localhost
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ELASTICSEARCH_URIS=http://localhost:9200
```

## 🚧 Roadmap & Future Development

See [ROADMAP.md](./ROADMAP.md) for detailed planned features and improvements.

### Immediate Next Steps (Must Complete First)
- **Message Reactions** - Toggle emoji reactions on messages
- **Polls System** - Create and vote on polls in conversations
- **Pin Messages** - Pin/unpin important messages in conversations
- **User Mentions** - Tag users with @username in messages
- **Message Search** - Full-text search within message content
- **Audit Logging** - Track all user activities and changes
- **Fan-out Presence System** - Scalable presence tracking for large user base

### High Priority
- Voice & Video calling integration (WebRTC)
- End-to-end encryption for messages
- Message forwarding functionality
- User blocking system

### Medium Priority
- Conversation export (JSON/PDF)
- Message scheduling
- Advanced search filters
- Chatbot integration

## ⚠️ Known Issues & Limitations

- **Presence System**: Only basic session-based presence implemented, fan-out system not completed
- **Search**: Only conversation search available, message content search not yet implemented
- **Reactions**: Backend endpoints exist but not fully functional
- **Polls**: Controller exists but service layer incomplete
- **Mentions**: Parser exists but full integration not complete
- Redis expiration listener may need fine-tuning for high load
- Elasticsearch sync may have delay in high-traffic scenarios

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👤 Author

**Cao Nhat Linh**

- GitHub: [@CaoNhatLinh](https://github.com/CaoNhatLinh)

## 🙏 Acknowledgments

- Spring Boot Team for the excellent framework
- Apache Cassandra, Kafka, and Elasticsearch communities
- STOMP.js and SockJS for WebSocket support
- All contributors and users of this project
