package com.chatapp.chat_service.repository;

import com.chatapp.chat_service.model.entity.UserPresence;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPresenceRepository extends CassandraRepository<UserPresence, UUID> {
    
    Optional<UserPresence> findByUserId(UUID userId);
    
    List<UserPresence> findByUserIdIn(List<UUID> userIds);
    
    @Query("SELECT * FROM user_presence WHERE is_online = true ALLOW FILTERING")
    List<UserPresence> findAllOnlineUsers();
    
    // Note: Complex joins like friendship queries are not efficiently supported in Cassandra
    // These will be implemented in the service layer using multiple queries or denormalization
}
