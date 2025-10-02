# Conversation Search API Documentation

## Elasticsearch Integration for Conversations

### Overview
The chat service now uses Elasticsearch to provide fast and efficient conversation search capabilities with the following features:

- **Full-text search** on conversation names
- **Type filtering** (GROUP or DM)
- **Soft delete filtering** (only returns non-deleted conversations)
- **Member-based filtering** (only returns conversations where the user is a member)
- **Pagination and sorting** by last message timestamp (newest first)

### API Endpoints

#### Search Conversations
```
GET /api/conversations/search
```

**Query Parameters:**
- `name` (optional): Search term for conversation name (case-insensitive partial match)
- `type` (optional): Filter by conversation type ("GROUP" or "DM")
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20, max: 100)
- `sort` (optional): Sort criteria (default: lastMessage.createdAt,desc)

**Headers:**
- `Authorization: Bearer <JWT_TOKEN>` (required)

**Example Requests:**

1. Search all conversations for user:
```bash
GET /api/conversations/search
```

2. Search conversations by name:
```bash
GET /api/conversations/search?name=project
```

3. Search only group conversations:
```bash
GET /api/conversations/search?type=GROUP
```

4. Search group conversations with name containing "meeting":
```bash
GET /api/conversations/search?name=meeting&type=GROUP
```

5. Search with pagination:
```bash
GET /api/conversations/search?name=project&page=0&size=10
```

**Response Format:**
```json
{
  "content": [
    {
      "conversationId": "uuid",
      "name": "Project Discussion",
      "type": "GROUP",
      "description": "Team project coordination",
      "avatar": "https://example.com/avatar.jpg",
      "createdAt": "2024-01-15T10:30:00Z",
      "lastMessage": {
        "messageId": "uuid",
        "senderId": "uuid",
        "content": "Last message content",
        "createdAt": "2024-01-15T14:45:30Z"
      },
      "createdBy": "uuid",
      "memberCount": 5,
      "memberIds": ["uuid1", "uuid2", "uuid3", "uuid4", "uuid5"]
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "orders": [
        {
          "property": "lastMessage.createdAt",
          "direction": "DESC"
        }
      ]
    }
  },
  "totalElements": 25,
  "totalPages": 2,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

### Technical Implementation

#### Elasticsearch Document Structure
```json
{
  "id": "conversation-uuid",
  "conversationId": "uuid",
  "name": "Conversation Name",
  "type": "GROUP",
  "isDeleted": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T12:00:00Z",
  "lastMessage": {
    "messageId": "uuid",
    "senderId": "uuid", 
    "content": "Last message content",
    "createdAt": "2024-01-15T14:45:30Z"
  },
  "createdBy": "uuid",
  "description": "Conversation description",
  "avatar": "https://example.com/avatar.jpg",
  "memberIds": ["uuid1", "uuid2", "uuid3"],
  "memberCount": 3
}
```

#### Key Features

1. **Automatic Indexing:**
   - Conversations are automatically indexed to Elasticsearch when created
   - Updates are synced when conversations are modified
   - Soft deletes are reflected in the search index

2. **Performance Optimizations:**
   - Uses Elasticsearch's native search capabilities
   - Avoids Cassandra's ALLOW FILTERING requirement
   - Efficient pagination and sorting
   - Member-based filtering without complex joins

3. **Data Consistency:**
   - Real-time updates when conversations are created/updated/deleted
   - Background sync service for data recovery
   - Automatic last_message_at updates when messages are sent

4. **Search Capabilities:**
   - Case-insensitive partial name matching
   - Exact type filtering
   - Member inclusion filtering
   - Deleted conversation exclusion
   - Sorted by activity (last_message_at)

### Configuration

Add to `application.properties`:
```properties
# Elasticsearch Configuration
elasticsearch.host=localhost
elasticsearch.port=9200
```

### Error Handling

The API returns standard HTTP status codes:
- `200 OK`: Successful search with results
- `400 Bad Request`: Invalid query parameters
- `401 Unauthorized`: Missing or invalid JWT token
- `500 Internal Server Error`: Elasticsearch or server issues

### Performance Considerations

1. **Index Size:** Elasticsearch indexes scale well with conversation volume
2. **Query Performance:** Sub-second response times for most search queries
3. **Memory Usage:** Elasticsearch caching improves repeated search performance
4. **Network Overhead:** Minimal JSON payload sizes with pagination

### Data Synchronization

- **Initial Sync:** Runs automatically on application startup
- **Real-time Updates:** Triggered by conversation CRUD operations
- **Message Activity:** Last message timestamps updated automatically
- **Consistency:** Background processes ensure data alignment between Cassandra and Elasticsearch
