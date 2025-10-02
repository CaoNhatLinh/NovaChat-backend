# NovaChat Backend - Roadmap & Development Plan

## 📋 Current Status (v0.8 - Beta)

### ✅ Completed Features
- [x] Real-time messaging with WebSocket (STOMP)
- [x] User authentication with JWT
- [x] Friend management system
- [x] Conversation management (DM & Group)
- [x] Typing indicators (Redis TTL-based)
- [x] Basic user presence system (session-based only)
- [x] Notification system (WebSocket + Kafka)
- [x] File attachments (images, videos, audio, files via Cloudinary)
- [x] Elasticsearch integration for conversation search
- [x] Redis caching
- [x] Kafka event streaming
- [x] Soft delete for conversations
- [x] Reply to messages
- [x] Read receipts
- [x] Message deletion
- [x] Immediate message echo (optimistic UI)

### 🚧 Partially Completed
- [ ] User presence system (basic only, fan-out system incomplete)
- [ ] Search functionality (conversations only, message content search missing)
- [ ] Message reactions (backend exists but not fully functional)
- [ ] Polls (controller exists, service incomplete)
- [ ] Mentions (parser exists, integration incomplete)
- [ ] Audit logging (table exists, integration incomplete)

### ❌ Not Started
- [ ] Pin/Unpin messages
- [ ] Voice/Video calling
- [ ] End-to-end encryption
- [ ] Message forwarding
- [ ] User blocking
- [ ] Advanced presence (fan-out system)

---

## 🚀 Future Development

### Phase 0: Complete Core Features (URGENT - Q4 2025)

> **These features should be completed before v1.0 release**

#### 🔴 Critical Priority (Must Complete)

##### 1. Message Reactions System
- [ ] Complete reaction toggle endpoint implementation
- [ ] Store reactions in Cassandra
- [ ] Broadcast reactions via WebSocket
- [ ] Display reaction counts
- [ ] Support multiple reactions per message
- [ ] Remove reaction functionality

**Status**: Backend exists but not functional  
**Estimated Time**: 2-3 days

##### 2. Polls System
- [ ] Complete poll service implementation
- [ ] Create poll with options
- [ ] Vote on poll options
- [ ] Real-time vote updates via WebSocket
- [ ] Poll results visualization data
- [ ] Close poll functionality

**Status**: Controller exists, service incomplete  
**Estimated Time**: 3-4 days

##### 3. Pin Messages
- [ ] Pin/Unpin message endpoints
- [ ] Store pinned messages
- [ ] Show pinned messages in conversation
- [ ] Limit pinned messages per conversation
- [ ] Notification for pinned messages

**Status**: Repository exists, service incomplete  
**Estimated Time**: 2 days

##### 4. User Mentions (@username)
- [ ] Complete mention detection in messages
- [ ] Store mentions in database
- [ ] Notification for mentioned users
- [ ] Highlight mentions in UI data
- [ ] Get all mentions for a user

**Status**: Parser exists, integration incomplete  
**Estimated Time**: 2-3 days

##### 5. Message Content Search
- [ ] Index messages in Elasticsearch
- [ ] Full-text search endpoint
- [ ] Search filters (date, sender, type)
- [ ] Search highlighting
- [ ] Pagination for search results

**Status**: Elasticsearch ready, message indexing missing  
**Estimated Time**: 3-4 days

##### 6. Audit Logging System
- [ ] Complete audit log service
- [ ] Log all user actions
- [ ] Query audit logs
- [ ] Audit log retention policy
- [ ] Admin audit log viewer

**Status**: Table exists, integration incomplete  
**Estimated Time**: 2-3 days

##### 7. Fan-out Presence System
- [ ] Implement pub/sub for presence updates
- [ ] Batch presence notifications
- [ ] Optimize for large friend lists
- [ ] Presence subscription management
- [ ] Reduce Redis memory usage

**Status**: Basic presence works, fan-out missing  
**Estimated Time**: 4-5 days

**Total Estimated Time**: 18-24 days (3-4 weeks)

---

### Phase 1: Core Enhancements (Q1 2026)

#### 🔴 High Priority

##### 1. Voice & Video Calling
- [ ] WebRTC integration for peer-to-peer calls
- [ ] Signaling server implementation
- [ ] TURN/STUN server setup
- [ ] Call history and logs
- [ ] Screen sharing support
- [ ] Group video calls (up to 10 participants)

**Technical Stack**: WebRTC, Jitsi Meet, or Twilio SDK

##### 2. End-to-End Encryption (E2EE)
- [ ] Signal Protocol implementation
- [ ] Key exchange mechanism
- [ ] Encrypted message storage
- [ ] Device verification
- [ ] Backup and recovery keys

**Technical Stack**: Signal Protocol, Libsignal-java

##### 3. Message Forwarding
- [ ] Forward to single conversation
- [ ] Forward to multiple conversations
- [ ] Forward with/without attribution
- [ ] Forward media attachments
- [ ] Forward limit per message

##### 4. Advanced Search
- [ ] Search messages by date range
- [ ] Search by message type (text, image, file, etc.)
- [ ] Search by sender
- [ ] Search attachments
- [ ] Full-text search optimization
- [ ] Search result highlighting

---

### Phase 2: User Experience (Q1 2026)

#### 🟡 Medium Priority

##### 1. User Blocking
- [ ] Block/Unblock users
- [ ] Prevent messages from blocked users
- [ ] Hide blocked users from search
- [ ] Block list management
- [ ] Report abuse functionality

##### 2. Conversation Features
- [ ] Conversation folders/categories
- [ ] Archive conversations
- [ ] Conversation labels/tags
- [ ] Mute notifications for specific conversations
- [ ] Custom conversation colors/themes

##### 3. Message Scheduling
- [ ] Schedule messages to send later
- [ ] Edit scheduled messages
- [ ] Delete scheduled messages
- [ ] Recurring scheduled messages
- [ ] Time zone support

##### 4. Export & Backup
- [ ] Export conversation to JSON
- [ ] Export conversation to PDF
- [ ] Export with media attachments
- [ ] Selective export (date range, message type)
- [ ] Automatic backup scheduling

##### 5. Custom Reactions
- [ ] Custom emoji reactions
- [ ] Animated reactions
- [ ] Reaction categories
- [ ] Frequently used reactions
- [ ] Reaction statistics

---

### Phase 3: Advanced Features (Q2 2026)

#### 🟢 Low Priority

##### 1. Message Translation
- [ ] Auto-translate messages
- [ ] Multi-language support
- [ ] Translation API integration (Google Translate, DeepL)
- [ ] Translation history
- [ ] Language detection

##### 2. Rich Media Support
- [ ] Sticker packs
- [ ] GIF integration (Giphy, Tenor)
- [ ] Custom stickers
- [ ] Animated emojis
- [ ] Voice messages with waveform

##### 3. Chatbot Integration
- [ ] Bot API framework
- [ ] Custom bot creation
- [ ] Bot marketplace
- [ ] AI-powered chatbot (GPT integration)
- [ ] Bot analytics

##### 4. Analytics Dashboard
- [ ] User activity statistics
- [ ] Message volume charts
- [ ] Peak usage times
- [ ] Popular conversations
- [ ] User engagement metrics
- [ ] System health monitoring

##### 5. Admin Panel
- [ ] User management
- [ ] Conversation moderation
- [ ] Content filtering
- [ ] Ban/Suspend users
- [ ] System configuration
- [ ] Audit log viewer

---

## 🔧 Technical Improvements

### Performance Optimization
- [ ] Database query optimization
- [ ] Redis cache strategy refinement
- [ ] Elasticsearch indexing optimization
- [ ] WebSocket connection pooling
- [ ] Load balancing for horizontal scaling
- [ ] CDN integration for media files

### Code Quality
- [ ] Increase test coverage to 80%+
- [ ] Add integration tests
- [ ] Add E2E tests
- [ ] Code documentation improvements
- [ ] API documentation with Swagger/OpenAPI
- [ ] Performance benchmarking

### Infrastructure
- [ ] Kubernetes deployment manifests
- [ ] CI/CD pipeline setup
- [ ] Docker Compose for development
- [ ] Monitoring with Prometheus & Grafana
- [ ] ELK stack for log aggregation
- [ ] Automated backups

### Security
- [ ] Rate limiting per user
- [ ] DDoS protection
- [ ] SQL injection prevention
- [ ] XSS protection
- [ ] CSRF tokens
- [ ] Security audit

---

## 🐛 Known Issues & Bug Fixes

### Critical
- [ ] **Fan-out presence system not implemented** - Basic presence works but doesn't scale
- [ ] **Message reactions not functional** - Backend exists but broken
- [ ] **Polls service incomplete** - Controller exists but no service implementation
- [ ] **Message search missing** - Elasticsearch ready but message indexing not done
- [ ] Optimize Redis expiration listener for high load
- [ ] Fix Elasticsearch sync delay under heavy traffic

### Major
- [ ] **Mentions integration incomplete** - Parser exists but not fully integrated
- [ ] **Audit logging incomplete** - Table exists but service not implemented
- [ ] **Pin messages not working** - Repository exists but service missing
- [ ] Presence subscription endpoints commented out (performance issues)
- [ ] Improve WebSocket reconnection logic
- [ ] Fix race condition in message delivery
- [ ] Handle duplicate message prevention

### Minor
- [ ] Improve error messages for better debugging
- [ ] Fix typing indicator occasionally stuck
- [ ] Optimize file upload size limits
- [ ] Better validation for message content

---

## 📊 Metrics & Goals

### Performance Targets
- Message delivery latency: < 100ms
- WebSocket connection handling: 10,000+ concurrent users
- Message throughput: 10,000+ messages/second
- Search response time: < 500ms
- API response time: < 200ms

### Scalability Goals
- Support 100,000+ active users
- Handle 1M+ messages per day
- 99.9% uptime
- Auto-scaling based on load

---

## 📝 Notes

### Dependencies to Update
- Spring Boot (check for latest version)
- Cassandra driver
- Elasticsearch client
- Kafka client
- Redis client

### Breaking Changes
- WebSocket endpoint changes (notify frontend team)
- Authentication flow updates
- API versioning strategy

### Documentation Needed
- API documentation (Swagger)
- WebSocket protocol specification
- Deployment guide
- Developer onboarding guide
- Troubleshooting guide

---

## 🤝 Contributing

Want to contribute to any of these features? Check our [Contributing Guidelines](./CONTRIBUTING.md) and pick an issue from our [GitHub Issues](https://github.com/CaoNhatLinh/NovaChat-backend/issues).

### How to Propose New Features
1. Open a GitHub Issue with `[Feature Request]` prefix
2. Describe the feature and use case
3. Wait for team discussion and approval
4. Start implementation after approval

---

**Last Updated**: October 2, 2025  
**Version**: 1.0.0  
**Maintainer**: Cao Nhat Linh
