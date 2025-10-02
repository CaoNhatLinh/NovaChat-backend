package com.chatapp.chat_service.repository;

import com.chatapp.chat_service.model.entity.FriendRequest;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends CassandraRepository<FriendRequest, UUID> {
    @Query("SELECT * FROM friend_requests_by_sender WHERE from_user_id = ?0")
    List<FriendRequest> findBySender(UUID fromUserId);

    @Query("SELECT * FROM friend_requests_by_receiver WHERE to_user_id = ?0 AND status = 'PENDING'")
    List<FriendRequest> findPendingByReceiver(UUID toUserId);
    @Query("SELECT * FROM friend_requests WHERE sender_id = ?0 AND receiver_id = ?1 AND status = ?2 ALLOW FILTERING")
    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, String status);

    // For checking existence
    default boolean existsBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, String status) {
        return findBySenderIdAndReceiverIdAndStatus(senderId, receiverId, status).isPresent();
    }
}