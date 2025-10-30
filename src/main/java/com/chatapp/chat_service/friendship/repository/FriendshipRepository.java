package com.chatapp.chat_service.friendship.repository;


import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import com.chatapp.chat_service.friendship.entity.Friendship;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends CassandraRepository<Friendship, Friendship.FriendshipKey> {



    @Query("SELECT * FROM friendships WHERE user_id = ?0 AND friend_id = ?1")
    Optional<Friendship> findByUserIdAndFriendId(UUID userId, UUID friendId);


    @Query("DELETE FROM friendships WHERE user_id = ?0 AND friend_id = ?1")
    void deleteByUserIdAndFriendId(UUID userId, UUID friendId);

    default boolean existsByUserIdAndFriendId(UUID userId, UUID friendId) {
        return findByUserIdAndFriendId(userId, friendId).isPresent();
    }
    @Query("SELECT * FROM friendships WHERE user_id = :userId AND status = :status ALLOW FILTERING")
    List<Friendship> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") String status
    );
    @Query("SELECT * FROM friendships WHERE friend_id = :friendId AND status = :status ALLOW FILTERING")
    List<Friendship> findByFriendIdAndStatus(
            @Param("friendId") UUID friendId,
            @Param("status") String status
    );
    default List<Friendship> findByUserOrFriendAndStatus(UUID userId, String status) {
        List<Friendship> result = new ArrayList<>();
        result.addAll(findByUserIdAndStatus(userId, status));
        result.addAll(findByFriendIdAndStatus(userId, status));
        return result;
    }

@Query("SELECT * FROM friendships WHERE friend_id = ?0 AND status = 'PENDING' ALLOW FILTERING")
    List<Friendship> findFriendRecevidRequest
(UUID friendId);

    @AllowFiltering
    @Query("SELECT * FROM friendships WHERE user_id = ?0 AND status = 'ACCEPTED'")

    List<Friendship> findByUserId(UUID userId);


    @Query("SELECT * FROM friendships WHERE user_id = ?0 AND status = 'PENDING' ALLOW FILTERING")
    List<Friendship> findFriendRequest(UUID userId);

    @Query("SELECT * FROM friendships WHERE user_id = ?0 AND status = 'BLOCKED'")
    List<Friendship> findFriendBlock(UUID userId);

    @Query("SELECT * FROM friendships WHERE user_id = ?0 AND friend_id = ?1")
    Optional<Friendship> findByUserAndFriend(UUID userId, UUID friendId);

    // Method to get accepted friend IDs for a user
    default List<UUID> findAcceptedFriendIds(UUID userId) {
        List<UUID> friendIds = new ArrayList<>();
        
        // Get friends where user is the requester
        List<Friendship> sentRequests = findByUserIdAndStatus(userId, "ACCEPTED");
        for (Friendship friendship : sentRequests) {
            friendIds.add(friendship.getFriendId());
        }
        
        // Get friends where user is the receiver
        List<Friendship> receivedRequests = findByFriendIdAndStatus(userId, "ACCEPTED");
        for (Friendship friendship : receivedRequests) {
            friendIds.add(friendship.getUserId());
        }
        
        return friendIds;
    }
}