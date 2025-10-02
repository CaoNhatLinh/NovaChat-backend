package com.chatapp.chat_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("friend_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {
    @PrimaryKey
    private UUID requestId;

    private UUID senderId;
    private UUID receiverId;
    private String status; // "PENDING", "ACCEPTED", "REJECTED"

    private Instant createdAt;
    private Instant updatedAt;
}