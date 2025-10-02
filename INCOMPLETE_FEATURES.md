# 🚧 Incomplete Features & Implementation Guide

> **Last Updated**: October 2, 2025  
> **Project Status**: v0.8 (Beta)

## 📋 Overview

This document lists all features that are **partially implemented** or **not yet implemented** in the NovaChat Backend. Use this as a development checklist to reach v1.0.

---

## ❌ Not Yet Implemented Features

### 1. Message Reactions System

**Status**: ❌ Backend code exists but not functional  
**Priority**: 🔴 Critical  
**Estimated Time**: 2-3 days

#### What Exists:
- ✅ `ReactionService.java` in `feature/reaction/`
- ✅ `MessageReactionRepository.java`
- ✅ `MessageReaction` entity

#### What's Missing:
- ❌ Working toggle reaction endpoint
- ❌ WebSocket broadcast for reactions
- ❌ Reaction count aggregation
- ❌ Multiple reactions per message support
- ❌ Frontend integration

#### Implementation Steps:
1. Fix `POST /api/messages/{conversationId}/{messageId}/reactions/{emoji}` endpoint
2. Implement reaction storage in Cassandra
3. Add WebSocket broadcast to conversation members
4. Add reaction notifications
5. Test with multiple users

#### Files to Modify:
```
src/main/java/com/chatapp/chat_service/
├── controller/MessageController.java
├── service/MessageService.java
├── feature/reaction/ReactionService.java
└── websocket/event/MessageReactionEvent.java
```

---

### 2. Polls System

**Status**: ❌ Controller exists, service incomplete  
**Priority**: 🔴 Critical  
**Estimated Time**: 3-4 days

#### What Exists:
- ✅ `PollController.java`
- ✅ `Poll` and `PollVote` entities
- ✅ `PollRepository.java` and `PollVoteRepository.java`

#### What's Missing:
- ❌ Complete `PollService.java` implementation
- ❌ Create poll functionality
- ❌ Vote on poll
- ❌ Real-time poll updates via WebSocket
- ❌ Poll results calculation
- ❌ Close poll functionality

#### Implementation Steps:
1. Complete `PollService.java` with all CRUD operations
2. Implement create poll endpoint
3. Implement vote endpoint with validation (one vote per user)
4. Add WebSocket broadcast for vote updates
5. Add poll results aggregation
6. Add poll notifications

#### Files to Modify:
```
src/main/java/com/chatapp/chat_service/
├── controller/PollController.java
├── service/MessagePollService.java (incomplete)
├── feature/poll/PollService.java
├── websocket/event/PollEvent.java (create)
└── model/dto/PollDto.java
```

---

### 3. Pin Messages

**Status**: ❌ Repository exists, service missing  
**Priority**: 🟡 High  
**Estimated Time**: 2 days

#### What Exists:
- ✅ `PinnedMessage` entity
- ✅ `PinnedMessageRepository.java`
- ✅ `PinService.java` skeleton in `feature/pin/`

#### What's Missing:
- ❌ Pin/Unpin endpoints
- ❌ Get pinned messages for conversation
- ❌ Pin limit validation (e.g., max 3 pins per conversation)
- ❌ Pin notifications
- ❌ WebSocket broadcast for pin events

#### Implementation Steps:
1. Complete `PinService.java`
2. Add pin/unpin endpoints in `MessageController`
3. Implement pin limit validation
4. Add WebSocket broadcast
5. Create pin notifications
6. Test pin functionality

#### Files to Create/Modify:
```
src/main/java/com/chatapp/chat_service/
├── controller/MessageController.java (add endpoints)
├── feature/pin/PinService.java (complete)
├── websocket/event/PinMessageEvent.java (create)
└── model/dto/PinnedMessageDto.java (create)
```

---

### 4. User Mentions (@username)

**Status**: 🚧 Parser exists, integration incomplete  
**Priority**: 🟡 High  
**Estimated Time**: 2-3 days

#### What Exists:
- ✅ `MentionParser.java` in `feature/mention/`
- ✅ `MentionService.java` skeleton
- ✅ `MessageMention` entity

#### What's Missing:
- ❌ Mention detection in message sending
- ❌ Store mentions in database
- ❌ Mention notifications
- ❌ Get all mentions for a user
- ❌ Highlight mentions in message response

#### Implementation Steps:
1. Integrate `MentionParser` into message sending flow
2. Store mentions when message is created
3. Create mention notifications
4. Add endpoint to get user's mentions
5. Include mention data in message response
6. Test mention functionality

#### Files to Modify:
```
src/main/java/com/chatapp/chat_service/
├── service/MessageService.java
├── feature/mention/MentionService.java
├── controller/MessageController.java (add get mentions endpoint)
└── model/dto/MessageResponseDto.java (add mentions field)
```

---

### 5. Message Content Search

**Status**: 🚧 Elasticsearch ready, message indexing missing  
**Priority**: 🟡 High  
**Estimated Time**: 3-4 days

#### What Exists:
- ✅ Elasticsearch infrastructure
- ✅ `ConversationDocument` for conversation search
- ✅ Elasticsearch configuration

#### What's Missing:
- ❌ `MessageDocument` entity
- ❌ Message indexing in Elasticsearch
- ❌ Message search endpoint
- ❌ Search filters (date, sender, type)
- ❌ Search highlighting
- ❌ Pagination for search results

#### Implementation Steps:
1. Create `MessageDocument` class
2. Create `MessageElasticsearchRepository`
3. Implement message indexing on create/update/delete
4. Create search service
5. Add search endpoint in controller
6. Implement search filters and pagination
7. Test search functionality

#### Files to Create:
```
src/main/java/com/chatapp/chat_service/elasticsearch/
├── document/MessageDocument.java (create)
├── repository/MessageElasticsearchRepository.java (create)
└── service/MessageElasticsearchService.java (create)
```

---

### 6. Audit Logging System

**Status**: 🚧 Table exists, integration incomplete  
**Priority**: 🟡 Medium  
**Estimated Time**: 2-3 days

#### What Exists:
- ✅ `AuditLog` entity
- ✅ `AuditLogRepository` in `feature/audit/`
- ✅ `AuditLogService` skeleton

#### What's Missing:
- ❌ Complete audit log service
- ❌ Aspect for automatic logging
- ❌ Log all user actions (login, message send, etc.)
- ❌ Query audit logs endpoint
- ❌ Audit log retention policy
- ❌ Admin audit viewer

#### Implementation Steps:
1. Complete `AuditLogService.java`
2. Create `@AuditLog` annotation
3. Create AOP aspect to intercept and log actions
4. Add audit log for critical operations
5. Create admin endpoint to view audit logs
6. Implement audit log cleanup job
7. Test audit logging

#### Files to Modify:
```
src/main/java/com/chatapp/chat_service/
├── feature/audit/AuditLogService.java
├── feature/audit/AuditLogAspect.java (create)
├── feature/audit/AuditLog.java (annotation, create)
├── controller/AdminController.java (create)
└── scheduler/AuditLogCleanupScheduler.java (create)
```

---

### 7. Fan-out Presence System

**Status**: 🚧 Basic presence works, fan-out missing  
**Priority**: 🔴 Critical  
**Estimated Time**: 4-5 days

#### What Exists:
- ✅ Basic session-based presence
- ✅ `PresenceService.java`
- ✅ Redis presence tracking
- ✅ WebSocket presence updates

#### What's Missing:
- ❌ Pub/sub for presence updates
- ❌ Batch presence notifications
- ❌ Subscription management (commented out)
- ❌ Optimize for large friend lists (1000+ friends)
- ❌ Reduce Redis memory usage
- ❌ Presence aggregation

#### Implementation Steps:
1. Implement Redis pub/sub for presence events
2. Create presence subscription manager
3. Batch presence updates (don't send individually)
4. Optimize Redis keys and TTL
5. Add presence aggregation service
6. Uncomment and fix presence subscription endpoints
7. Test with large friend lists (1000+ users)

#### Files to Modify:
```
src/main/java/com/chatapp/chat_service/
├── service/presence/PresenceService.java
├── service/subscription/PresenceSubscriptionService.java
├── controller/WebSocketChatController.java (uncomment)
├── redis/publisher/PresencePublisher.java (create)
└── redis/subscriber/PresenceSubscriber.java (create)
```

---

### 8. Voice & Video Calling

**Status**: ❌ Not started  
**Priority**: 🟡 High (Phase 1)  
**Estimated Time**: 2-3 weeks

#### What's Missing:
- ❌ WebRTC signaling server
- ❌ TURN/STUN server setup
- ❌ Call initiation endpoints
- ❌ Call state management
- ❌ Call history
- ❌ Screen sharing support

#### Implementation Notes:
- Consider using Jitsi Meet or Twilio SDK
- May require separate signaling server
- Need to handle ICE candidates
- Store call history in Cassandra

---

### 9. End-to-End Encryption

**Status**: ❌ Not started  
**Priority**: 🔴 Critical (Phase 1)  
**Estimated Time**: 3-4 weeks

#### What's Missing:
- ❌ Signal Protocol implementation
- ❌ Key exchange mechanism
- ❌ Encrypted message storage
- ❌ Device verification
- ❌ Backup keys

#### Implementation Notes:
- Use Libsignal-java library
- Requires reworking message storage
- Frontend also needs major changes

---

## 📊 Priority Summary

### Must Complete for v1.0 (Critical)
1. ✅ Fan-out Presence System (4-5 days)
2. ✅ Message Reactions (2-3 days)
3. ✅ Polls System (3-4 days)

### Should Complete for v1.0 (High)
4. ✅ Pin Messages (2 days)
5. ✅ User Mentions (2-3 days)
6. ✅ Message Content Search (3-4 days)

### Nice to Have for v1.0 (Medium)
7. ✅ Audit Logging (2-3 days)

### Future Releases (v2.0+)
8. Voice & Video Calling
9. End-to-End Encryption
10. Message Forwarding
11. User Blocking
12. Advanced Analytics

---

## 🎯 Recommended Implementation Order

1. **Week 1-2**: Fan-out Presence System (Critical for scale)
2. **Week 2**: Message Reactions (Highly requested)
3. **Week 3**: Polls System + Pin Messages
4. **Week 4**: User Mentions + Message Search
5. **Week 5**: Audit Logging + Bug fixes
6. **Week 6**: Testing + Documentation

**Total Time to v1.0**: ~6 weeks

---

## 📝 Notes for Developers

### Testing Checklist
- [ ] Unit tests for each service
- [ ] Integration tests for endpoints
- [ ] WebSocket functionality tests
- [ ] Load testing for scalability
- [ ] Security testing

### Documentation Updates Needed
- [ ] Update API_REFERENCE.md
- [ ] Update README.md features list
- [ ] Add usage examples in docs/
- [ ] Update ROADMAP.md
- [ ] Update CONTRIBUTING.md

### Code Quality
- [ ] Add JavaDoc comments
- [ ] Follow naming conventions
- [ ] Handle errors properly
- [ ] Add logging statements
- [ ] Write clean, maintainable code

---

## 🤝 Contributing

Want to help complete these features? See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

**Tips**:
- Start with smaller features (Pin Messages, Mentions)
- Test thoroughly before submitting PR
- Update documentation
- Follow existing code patterns
- Ask questions in GitHub Discussions

---

**Last Updated**: October 2, 2025  
**Maintainer**: Cao Nhat Linh  
**Status**: Work in Progress - v0.8 → v1.0
