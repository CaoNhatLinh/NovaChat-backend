# NovaChat Backend

A real-time chat application backend built with Spring Boot, featuring WebSocket communication, Elasticsearch search, and comprehensive notification system.

## 🚀 Features

- **Real-time Messaging**: WebSocket-based instant messaging with typing indicators
- **User Presence System**: Track online/offline status with session management
- **Advanced Search**: Elasticsearch integration for fast conversation and message search
- **Notification System**: Real-time notifications via WebSocket and Kafka
- **Friend Management**: Send/accept friend requests and manage relationships
- **Conversation Management**: Support for both DM and group conversations with soft delete
- **Message Reactions**: React to messages with emoji support
- **Secure Authentication**: JWT-based authentication with Spring Security

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

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### Conversations
- `GET /api/conversations/my` - Get user conversations
- `GET /api/conversations/search` - Search conversations (Elasticsearch)
- `POST /api/conversations` - Create new conversation
- `DELETE /api/conversations/{id}` - Soft delete conversation

### Messages
- `GET /api/messages/{conversationId}` - Get messages
- `POST /api/messages/{conversationId}` - Send message
- `POST /api/messages/{conversationId}/{messageId}/reactions/{emoji}` - Toggle reaction

### Notifications
- `GET /api/notifications` - Get user notifications
- `GET /api/notifications/unread/count` - Get unread count
- `PUT /api/notifications/{id}/read` - Mark as read

### Friends
- `GET /api/friends` - Get friends list
- `POST /api/friends/request/{userId}` - Send friend request
- `PUT /api/friends/accept/{userId}` - Accept friend request

For detailed API documentation, see the [docs](./docs) folder.

## 📖 Documentation

Detailed documentation is available in the `/docs` folder:

- [Complete Notification Service Guide](./docs/COMPLETE_NOTIFICATION_SERVICE_GUIDE.md)
- [WebSocket Presence System Guide](./docs/COMPREHENSIVE_WEBSOCKET_PRESENCE_SYSTEM.md)
- [Elasticsearch Search API](./docs/ELASTICSEARCH_SEARCH_API.md)
- [Frontend Integration Guide](./docs/FRONTEND_INTEGRATION.md)
- [Kafka Configuration](./docs/KAFKA_FIX_DOCUMENTATION.md)
- And more...

## 🔌 WebSocket Endpoints

Connect to WebSocket at: `ws://localhost:8084/ws`

### Subscribe Channels:
- `/user/queue/messages` - Receive personal messages
- `/user/queue/notifications` - Receive notifications
- `/user/queue/online-status` - Receive presence updates
- `/topic/conversation/{conversationId}` - Conversation-specific updates

### Send Messages:
- `/app/message.send` - Send message
- `/app/typing.start` - Start typing indicator
- `/app/typing.stop` - Stop typing indicator

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
- All contributors and users of this project
