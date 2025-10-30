# API Quản Lý Phòng Chat (Group/Channel Management)

## Tổng quan
API này cung cấp đầy đủ các chức năng quản lý phòng chat (group và channel), bao gồm:
- Quản lý thành viên (mời, kick, danh sách)
- Quản lý quyền (chuyển chủ phòng, trao/thu hồi quyền admin)
- Quản lý invitation links (tạo, xóa, vô hiệu hóa)
- Cập nhật thông tin phòng
- Xóa phòng

## Phân quyền

### Roles
- **owner**: Chủ phòng (người tạo hoặc được chuyển quyền)
- **admin**: Quản trị viên (được owner trao quyền)
- **member**: Thành viên thông thường
- **moderator**: Người kiểm duyệt (tính năng mở rộng)

### Quyền hạn theo role

| Chức năng | Owner | Admin | Member |
|-----------|-------|-------|--------|
| Xem danh sách members | ✅ | ✅ | ✅ |
| Mời thành viên | ✅ | ✅ | ❌ |
| Kick thành viên | ✅ | ✅ (không kick được admin) | ❌ |
| Tạo invitation link | ✅ | ✅ | ❌ |
| Xóa link của người khác | ✅ | ✅ | ❌ |
| Xóa link của mình | ✅ | ✅ | ✅ |
| Cập nhật thông tin phòng | ✅ | ✅ | ❌ |
| Trao quyền admin | ✅ | ❌ | ❌ |
| Thu hồi quyền admin | ✅ | ❌ | ❌ |
| Chuyển quyền chủ phòng | ✅ | ❌ | ❌ |
| Xóa phòng | ✅ | ❌ | ❌ |
| Rời phòng | ❌ (phải chuyển quyền trước) | ✅ | ✅ |

## API Endpoints

### 1. Quản lý Members

#### 1.1. Lấy danh sách members
```http
GET /api/conversations/{conversationId}/management/members
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "userId": "uuid",
    "conversationId": "uuid",
    "role": "owner",
    "joinedAt": "2025-10-29T10:00:00Z",
    "username": "john_doe",
    "displayName": "John Doe",
    "avatarUrl": "https://...",
    "isOnline": true,
    "lastSeen": "2025-10-29T10:00:00Z"
  }
]
```

#### 1.2. Thêm thành viên
```http
POST /api/conversations/{conversationId}/management/members/add
Authorization: Bearer {token}
Content-Type: application/json

{
  "memberIds": ["uuid1", "uuid2", "uuid3"]
}
```

**Quyền cần có:** Owner hoặc Admin

**Response:** `200 OK` - "Đã thêm thành viên thành công"

#### 1.3. Kick thành viên
```http
DELETE /api/conversations/{conversationId}/management/members/{memberId}
Authorization: Bearer {token}
```

**Quyền cần có:** Owner hoặc Admin

**Lưu ý:**
- Không thể kick owner
- Admin không thể kick admin khác (chỉ owner mới được)

**Response:** `200 OK` - "Đã kick thành viên thành công"

#### 1.4. Rời khỏi phòng
```http
POST /api/conversations/{conversationId}/management/leave
Authorization: Bearer {token}
```

**Lưu ý:** Owner không được phép rời phòng. Phải chuyển quyền chủ phòng cho người khác trước.

**Response:** `200 OK` - "Đã rời khỏi phòng thành công"

### 2. Quản lý Quyền

#### 2.1. Chuyển quyền chủ phòng
```http
POST /api/conversations/{conversationId}/management/transfer-ownership
Authorization: Bearer {token}
Content-Type: application/json

{
  "newOwnerId": "uuid"
}
```

**Quyền cần có:** Owner hiện tại

**Lưu ý:** Owner cũ sẽ tự động trở thành admin

**Response:** `200 OK` - "Đã chuyển quyền chủ phòng thành công"

#### 2.2. Trao quyền admin
```http
POST /api/conversations/{conversationId}/management/grant-admin
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": "uuid"
}
```

**Quyền cần có:** Owner

**Response:** `200 OK` - "Đã trao quyền admin thành công"

#### 2.3. Thu hồi quyền admin
```http
POST /api/conversations/{conversationId}/management/revoke-admin
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": "uuid"
}
```

**Quyền cần có:** Owner

**Response:** `200 OK` - "Đã thu hồi quyền admin thành công"

### 3. Quản lý Invitation Links

#### 3.1. Tạo invitation link
```http
POST /api/conversations/{conversationId}/management/invitations
Authorization: Bearer {token}
Content-Type: application/json

{
  "expiresInHours": 24,  // Optional, default: 24
  "maxUses": 10          // Optional, null = unlimited
}
```

**Quyền cần có:** Owner hoặc Admin

**Response:**
```json
{
  "linkId": "uuid",
  "conversationId": "uuid",
  "linkToken": "abc123...",
  "fullLink": "http://localhost:3000/join/abc123...",
  "createdBy": "uuid",
  "createdByUsername": "john_doe",
  "createdAt": "2025-10-29T10:00:00Z",
  "expiresAt": "2025-10-30T10:00:00Z",
  "isActive": true,
  "maxUses": 10,
  "usedCount": 0,
  "isExpired": false,
  "canDelete": true
}
```

#### 3.2. Lấy danh sách invitation links
```http
GET /api/conversations/{conversationId}/management/invitations
Authorization: Bearer {token}
```

**Response:** Array of InvitationLinkDto (như trên)

#### 3.3. Xóa invitation link
```http
DELETE /api/conversations/{conversationId}/management/invitations/{linkId}
Authorization: Bearer {token}
```

**Quyền cần có:** Owner, Admin, hoặc người tạo link

**Response:** `200 OK` - "Đã xóa link mời thành công"

#### 3.4. Vô hiệu hóa invitation link
```http
PUT /api/conversations/{conversationId}/management/invitations/{linkId}/deactivate
Authorization: Bearer {token}
```

**Quyền cần có:** Owner, Admin, hoặc người tạo link

**Response:** `200 OK` - "Đã vô hiệu hóa link mời thành công"

#### 3.5. Join phòng qua invitation link
```http
POST /api/conversations/{conversationId}/management/invitations/join/{linkToken}
Authorization: Bearer {token}
```

**Response:** `200 OK` - "Đã tham gia phòng thành công"

**Lỗi có thể xảy ra:**
- `400 Bad Request`: Link đã hết hạn, bị vô hiệu hóa, hoặc đã đạt giới hạn sử dụng
- `400 Bad Request`: Bạn đã là thành viên của phòng này
- `404 Not Found`: Link không tồn tại

### 4. Quản lý Thông tin Phòng

#### 4.1. Cập nhật thông tin phòng
```http
PUT /api/conversations/{conversationId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "New Room Name",           // Optional
  "description": "New description",  // Optional
  "backgroundUrl": "https://..."     // Optional
}
```

**Quyền cần có:** Owner hoặc Admin

**Response:** Conversation object đã được cập nhật

#### 4.2. Xóa phòng (soft delete)
```http
DELETE /api/conversations/{conversationId}
Authorization: Bearer {token}
```

**Quyền cần có:** Owner

**Lưu ý:** Đây là soft delete, phòng có thể được khôi phục sau

**Response:** `200 OK` - "Conversation đã được xóa thành công"

#### 4.3. Xóa phòng vĩnh viễn
```http
DELETE /api/conversations/{conversationId}/permanent
Authorization: Bearer {token}
```

**Quyền cần có:** Owner

**Lưu ý:** 
- Hành động này không thể hoàn tác
- Không thể xóa phòng DM
- Tất cả members và dữ liệu sẽ bị xóa

**Response:** `200 OK` - "Conversation đã được xóa vĩnh viễn"

#### 4.4. Khôi phục phòng đã xóa
```http
PUT /api/conversations/{conversationId}/restore
Authorization: Bearer {token}
```

**Quyền cần có:** Owner

**Response:** `200 OK` - "Conversation đã được khôi phục thành công"

## Error Responses

### 403 Forbidden
```json
{
  "message": "Bạn không có quyền thực hiện hành động này"
}
```

### 404 Not Found
```json
{
  "message": "Conversation không tồn tại"
}
```

### 400 Bad Request
```json
{
  "message": "Lỗi cụ thể (VD: Owner không thể rời khỏi phòng)"
}
```

## Use Cases

### 1. Tạo và quản lý phòng group mới

```javascript
// 1. Tạo phòng
POST /api/conversations/create
{
  "name": "Team Discussion",
  "type": "group",
  "description": "Team chat room",
  "memberIds": ["user1", "user2", "user3"]
}

// 2. Trao quyền admin cho member
POST /api/conversations/{id}/management/grant-admin
{
  "userId": "user2"
}

// 3. Tạo invitation link để mời thêm người
POST /api/conversations/{id}/management/invitations
{
  "expiresInHours": 48,
  "maxUses": 5
}

// 4. Cập nhật avatar phòng
PUT /api/conversations/{id}
{
  "backgroundUrl": "https://example.com/room-bg.jpg"
}
```

### 2. Chuyển giao quyền chủ phòng

```javascript
// 1. Trao quyền admin cho người nhận trước (optional nhưng recommended)
POST /api/conversations/{id}/management/grant-admin
{
  "userId": "newOwnerId"
}

// 2. Chuyển quyền chủ phòng
POST /api/conversations/{id}/management/transfer-ownership
{
  "newOwnerId": "newOwnerId"
}
```

### 3. Quản lý invitation links

```javascript
// 1. Tạo link mời có thời hạn
POST /api/conversations/{id}/management/invitations
{
  "expiresInHours": 24,
  "maxUses": 10
}

// 2. Xem tất cả links đang active
GET /api/conversations/{id}/management/invitations

// 3. Vô hiệu hóa link khi không cần nữa
PUT /api/conversations/{id}/management/invitations/{linkId}/deactivate

// 4. Xóa link hoàn toàn
DELETE /api/conversations/{id}/management/invitations/{linkId}
```

## Frontend Integration

### TypeScript Types

```typescript
// File: conversation.ts đã được cập nhật với các types:

export interface ConversationMember {
  userId: string;
  conversationId: string;
  role: 'owner' | 'admin' | 'member' | 'moderator';
  joinedAt: string;
  username?: string;
  displayName?: string;
  avatarUrl?: string;
  isOnline?: boolean;
  lastSeen?: string;
}

export interface InvitationLink {
  linkId: string;
  conversationId: string;
  linkToken: string;
  fullLink: string;
  createdBy: string;
  createdByUsername: string;
  createdAt: string;
  expiresAt: string;
  isActive: boolean;
  maxUses?: number;
  usedCount: number;
  isExpired: boolean;
  canDelete: boolean;
}
```

### API Service Example

```typescript
// conversationManagementApi.ts
import axios from 'axios';

export const conversationManagementApi = {
  // Members
  getMembers: (conversationId: string) => 
    axios.get(`/api/conversations/${conversationId}/management/members`),
  
  addMembers: (conversationId: string, memberIds: string[]) =>
    axios.post(`/api/conversations/${conversationId}/management/members/add`, { memberIds }),
  
  kickMember: (conversationId: string, memberId: string) =>
    axios.delete(`/api/conversations/${conversationId}/management/members/${memberId}`),
  
  leaveConversation: (conversationId: string) =>
    axios.post(`/api/conversations/${conversationId}/management/leave`),
  
  // Permissions
  transferOwnership: (conversationId: string, newOwnerId: string) =>
    axios.post(`/api/conversations/${conversationId}/management/transfer-ownership`, { newOwnerId }),
  
  grantAdmin: (conversationId: string, userId: string) =>
    axios.post(`/api/conversations/${conversationId}/management/grant-admin`, { userId }),
  
  revokeAdmin: (conversationId: string, userId: string) =>
    axios.post(`/api/conversations/${conversationId}/management/revoke-admin`, { userId }),
  
  // Invitation Links
  createInvitationLink: (conversationId: string, data: CreateInvitationLinkRequest) =>
    axios.post(`/api/conversations/${conversationId}/management/invitations`, data),
  
  getInvitationLinks: (conversationId: string) =>
    axios.get(`/api/conversations/${conversationId}/management/invitations`),
  
  deleteInvitationLink: (conversationId: string, linkId: string) =>
    axios.delete(`/api/conversations/${conversationId}/management/invitations/${linkId}`),
  
  joinViaLink: (conversationId: string, linkToken: string) =>
    axios.post(`/api/conversations/${conversationId}/management/invitations/join/${linkToken}`)
};
```

## Database Schema

### Table: invitation_links

```sql
CREATE TABLE invitation_links (
    link_id UUID PRIMARY KEY,
    conversation_id UUID,
    link_token TEXT,
    created_by UUID,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN,
    max_uses INT,
    used_count INT
);

-- Indexes
CREATE INDEX idx_invitation_links_conversation_id ON invitation_links (conversation_id);
CREATE INDEX idx_invitation_links_link_token ON invitation_links (link_token);
```

### Table: conversation_members (existing)

```sql
-- Role field updated to support: owner, admin, member, moderator
role TEXT  -- 'owner' | 'admin' | 'member' | 'moderator'
```

## Notes

1. **Security:**
   - Tất cả API đều yêu cầu authentication token
   - Mỗi action đều được kiểm tra quyền trước khi thực hiện
   - Invitation links có thời gian hết hạn và giới hạn số lần sử dụng

2. **Performance:**
   - Conversation data được cache trong Redis
   - DM conversations được cache riêng với key pattern: `dmChat:{userId1}:{userId2}`
   - Cache tự động bị xóa khi có thay đổi

3. **Best Practices:**
   - Luôn trao quyền admin trước khi chuyển ownership
   - Sử dụng invitation links với thời hạn hợp lý (24-48 giờ)
   - Giới hạn số lần sử dụng link để tránh spam
   - Xóa hoặc vô hiệu hóa links cũ định kỳ

4. **Limitations:**
   - Owner không thể tự rời phòng (phải chuyển quyền trước)
   - Admin không thể kick admin khác
   - Không thể xóa vĩnh viễn phòng DM
   - Member thông thường không có quyền quản lý
