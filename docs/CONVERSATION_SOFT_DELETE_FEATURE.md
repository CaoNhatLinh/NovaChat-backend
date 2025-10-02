## Conversation Soft Delete Feature

### Thay đổi đã thực hiện:

#### 1. **ConversationRepository.java**
- ✅ Thêm method `findByConversationIdAndNotDeleted()` 
- ✅ Thêm method `findAllByIdAndNotDeleted()` cho bulk query
- ✅ Thêm method `findByConversationIdAndTypeAndNotDeleted()` cho DM conversation

#### 2. **ConversationService.java**
- ✅ Cập nhật `getUserConversations()` để chỉ trả về conversation không bị xóa
- ✅ Cập nhật `getConversationById()` để kiểm tra `is_deleted = false`
- ✅ Cập nhật `findPrivateConversation()` để filter theo type và is_deleted
- ✅ Thêm method `deleteConversation()` cho soft delete
- ✅ Thêm method `restoreConversation()` để khôi phục conversation

#### 3. **ConversationController.java**
- ✅ Thêm endpoint `DELETE /{conversationId}` để soft delete
- ✅ Thêm endpoint `PUT /{conversationId}/restore` để khôi phục

### Cách sử dụng:

#### Soft Delete Conversation:
```bash
DELETE /api/conversations/{conversationId}
Authorization: Bearer <token>
```

#### Khôi phục Conversation:
```bash
PUT /api/conversations/{conversationId}/restore
Authorization: Bearer <token>
```

#### Lấy danh sách Conversation (chỉ trả về không bị xóa):
```bash
GET /api/conversations/my
Authorization: Bearer <token>
```

#### Tìm DM Conversation (chỉ trả về không bị xóa):
```bash
GET /api/conversations/dm?userId1={userId1}&userId2={userId2}
```

### Lưu ý quan trọng:

1. **Quyền hạn**: Chỉ người tạo conversation (owner) mới có thể xóa hoặc khôi phục
2. **Soft Delete**: Conversation không bị xóa vĩnh viễn, chỉ set `is_deleted = true`
3. **Cache**: Khi xóa conversation, cache Redis liên quan cũng được clear
4. **Backward Compatibility**: Tất cả API hiện tại sẽ tự động filter conversation đã bị xóa

### Testing:

1. Tạo một conversation mới
2. Verify conversation xuất hiện trong danh sách
3. Soft delete conversation bằng DELETE API
4. Verify conversation không còn xuất hiện trong danh sách
5. Khôi phục conversation bằng PUT API
6. Verify conversation xuất hiện lại trong danh sách

### Database Schema:

Conversation table đã có sẵn column `is_deleted` (boolean):
```sql
CREATE TABLE conversations (
    conversation_id UUID PRIMARY KEY,
    type TEXT,
    name TEXT,
    description TEXT,
    is_deleted BOOLEAN,
    last_message TEXT,
    created_by UUID,
    background_url TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Frontend Integration:

Frontend có thể thêm các API calls mới:

```typescript
// In conversationApi.ts
export const deleteConversation = async (conversationId: string): Promise<void> => {
  await api.delete(`/conversations/${conversationId}`);
};

export const restoreConversation = async (conversationId: string): Promise<void> => {
  await api.put(`/conversations/${conversationId}/restore`);
};
```

Tất cả các thay đổi này đảm bảo rằng:
- ✅ Conversation đã xóa không xuất hiện trong danh sách
- ✅ Không thể truy cập vào conversation đã xóa
- ✅ Có thể khôi phục conversation nếu cần
- ✅ Bảo mật: Chỉ owner mới có quyền xóa/khôi phục
