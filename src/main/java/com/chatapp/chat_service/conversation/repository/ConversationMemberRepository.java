package com.chatapp.chat_service.conversation.repository;

import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chatapp.chat_service.conversation.entity.ConversationMembers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface ConversationMemberRepository extends CassandraRepository<ConversationMembers, ConversationMembers.ConversationMemberKey> {

    @AllowFiltering
    List<ConversationMembers> findByKeyUserId(UUID userId);
    
    @AllowFiltering  
    List<ConversationMembers> findByKeyConversationId(UUID conversationId);
    
    boolean existsByKeyConversationIdAndKeyUserId(UUID conversationId, UUID userId);

    @Query("SELECT * FROM conversation_members WHERE user_id = :userId ALLOW FILTERING")
    List<ConversationMembers> findByUserId(@Param("userId") UUID userId);
    
    // Helper method to get conversation IDs
    default List<UUID> findConversationIdsByUserId(UUID userId) {
        return findByUserId(userId).stream()
            .map(member -> member.getKey().getConversationId())
            .collect(Collectors.toList());
    }
    
    // Helper method for presence service
    default List<ConversationMembers> findByConversationId(UUID conversationId) {
        return findByKeyConversationId(conversationId);
    }
}
