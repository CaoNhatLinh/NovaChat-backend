package com.chatapp.chat_service.presence.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import com.chatapp.chat_service.presence.entity.UserPresence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPresenceRepository extends CassandraRepository<UserPresence, UUID> {
    
    Optional<UserPresence> findByUserId(UUID userId);
    
    List<UserPresence> findByUserIdIn(List<UUID> userIds);
    
    @Query("SELECT * FROM user_presence WHERE is_online = true ALLOW FILTERING")
    List<UserPresence> findAllOnlineUsers();

}
