## Test Case: Đảm bảo tìm phòng chat giữa 2 người chỉ trả về phòng chưa xóa

### Scenario 1: Tìm DM conversation bình thường (chưa bị xóa)

**Test Steps:**
1. Tạo DM conversation giữa User A và User B
2. Gọi API: `GET /api/conversations/dm?userId1={userA}&userId2={userB}`
3. Verify: API trả về conversation

**Expected Result:** ✅ API trả về conversation với `is_deleted = false`

### Scenario 2: Tìm DM conversation đã bị soft delete

**Test Steps:**
1. Tạo DM conversation giữa User A và User B
2. Soft delete conversation: `DELETE /api/conversations/{conversationId}`
3. Gọi API: `GET /api/conversations/dm?userId1={userA}&userId2={userB}`
4. Verify: API trả về 404 Not Found

**Expected Result:** ✅ API không trả về conversation đã bị xóa

### Scenario 3: Khôi phục và tìm lại DM conversation

**Test Steps:**
1. Tạo DM conversation giữa User A và User B
2. Soft delete conversation: `DELETE /api/conversations/{conversationId}`  
3. Verify: Không tìm thấy conversation
4. Khôi phục: `PUT /api/conversations/{conversationId}/restore`
5. Gọi API: `GET /api/conversations/dm?userId1={userA}&userId2={userB}`
6. Verify: API trả về conversation

**Expected Result:** ✅ Conversation xuất hiện lại sau khi khôi phục

### Scenario 4: Cache behavior với conversation đã xóa

**Test Steps:**
1. Tạo DM conversation giữa User A và User B
2. Gọi API tìm DM (để cache conversation)
3. Soft delete conversation
4. Gọi API tìm DM lần nữa
5. Verify: Cache đã được clear, API trả về 404

**Expected Result:** ✅ Cache được clear khi conversation bị xóa

### Technical Implementation Verification:

#### ✅ ConversationRepository
- `findByConversationIdAndTypeAndNotDeleted()` đã được implement
- Query có điều kiện: `is_deleted = false AND type = 'dm'`

#### ✅ ConversationService  
- `findPrivateConversation()` sử dụng `findByConversationIdAndTypeAndNotDeleted()`
- `findPrivateConversationWithCache()` kiểm tra cache và `is_deleted` flag
- `deleteConversation()` clear cache DM khi xóa
- `clearDmCache()` helper method để clear cache

#### ✅ ConversationController
- API `/dm` sử dụng `findPrivateConversationWithCache()`
- Comment rõ ràng: "chỉ trả về phòng chưa bị xóa"

### Database Query Examples:

```sql
-- Query được sử dụng trong findByConversationIdAndTypeAndNotDeleted
SELECT * FROM conversations 
WHERE conversation_id = ? 
AND type = 'dm' 
AND is_deleted = false

-- Query để lấy members của conversation (cho cache clearing)
SELECT * FROM conversation_members 
WHERE conversation_id = ?
```

### API Test Examples:

```bash
# Test 1: Tìm DM conversation bình thường
curl -X GET "/api/conversations/dm?userId1=uuid1&userId2=uuid2"
# Expected: 200 OK with conversation data

# Test 2: Tìm DM conversation đã xóa  
curl -X DELETE "/api/conversations/{conversationId}"
curl -X GET "/api/conversations/dm?userId1=uuid1&userId2=uuid2"
# Expected: 404 Not Found

# Test 3: Khôi phục và tìm lại
curl -X PUT "/api/conversations/{conversationId}/restore"
curl -X GET "/api/conversations/dm?userId1=uuid1&userId2=uuid2"  
# Expected: 200 OK with conversation data
```

### Performance Considerations:

1. **Cache First**: Check Redis cache trước khi query database
2. **Cache Invalidation**: Tự động clear cache khi conversation bị xóa
3. **Optimized Query**: Sử dụng composite condition trong single query
4. **Error Handling**: Graceful fallback nếu cache có lỗi

### Security Considerations:

1. **Authorization**: Chỉ owner có thể xóa/khôi phục conversation  
2. **Data Integrity**: Soft delete giữ nguyên data, không mất thông tin
3. **Audit Trail**: Log các action xóa/khôi phục conversation

Tất cả implementation đảm bảo rằng **chỉ conversation chưa bị xóa** (`is_deleted = false`) mới được trả về từ API tìm DM conversation.
