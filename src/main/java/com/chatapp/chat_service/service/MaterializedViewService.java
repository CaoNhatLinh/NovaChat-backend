package com.chatapp.chat_service.service;



import com.chatapp.chat_service.model.entity.Friendship;
import com.chatapp.chat_service.repository.FriendshipRepository;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MaterializedViewService {
    private final FriendshipRepository friendshipRepository;
    private final CassandraOperations cassandraTemplate;

    public MaterializedViewService(FriendshipRepository friendshipRepository,
                                   CassandraOperations cassandraTemplate) {
        this.friendshipRepository = friendshipRepository;
        this.cassandraTemplate = cassandraTemplate;
    }

    public void updateFriendshipView(UUID senderId, UUID receiverId, String status) {
        // Update materialized views based on status
        switch (status) {
            case "ACCEPTED":
                updateAcceptedFriendships(senderId, receiverId);
                break;
            case "REJECTED":
                updateRejectedFriendships(senderId, receiverId);
                break;
            case "BLOCKED":
                updateBlockedRelationships(senderId, receiverId);
                break;
            default:
                throw new IllegalArgumentException("Invalid friendship status: " + status);
        }
    }

    private void updateAcceptedFriendships(UUID user1, UUID user2) {
        // Update accepted_friendships materialized view
        // This would normally be automatic in Cassandra, but we can add custom logic here
        Friendship friendship1 = friendshipRepository.findByUserAndFriend(user1, user2)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));

        Friendship friendship2 = friendshipRepository.findByUserAndFriend(user2, user1)
                .orElseThrow(() -> new RuntimeException("Inverse friendship not found"));

        // Additional business logic if needed
    }

    private void updateRejectedFriendships(UUID senderId, UUID receiverId) {
        // Update any views tracking rejected requests
        // Could be used for analytics or preventing repeated requests
    }

    private void updateBlockedRelationships(UUID blockerId, UUID blockedId) {
        // Update blocked_relationships materialized view
        String query = "INSERT INTO chat_app.blocked_relationships (user_id, friend_id, status, updated_at) " +
                "VALUES (?, ?, 'BLOCKED', toTimestamp(now()))";

        cassandraTemplate.getCqlOperations().execute(query, blockerId, blockedId);
    }

    public void updateLastMessagePreview(UUID conversationId, UUID messageId, String preview) {
        // Update user_conversations materialized view
        String query = "UPDATE chat_app.user_conversations " +
                "SET last_message_preview = ?, last_message_time = toTimestamp(now()) " +
                "WHERE user_id IN (SELECT user_id FROM chat_app.conversation_members WHERE conversation_id = ?) " +
                "AND conversation_id = ?";

        cassandraTemplate.getCqlOperations().execute(query, preview, conversationId, conversationId);
    }
}