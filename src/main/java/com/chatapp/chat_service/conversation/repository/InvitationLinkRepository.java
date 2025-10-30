package com.chatapp.chat_service.conversation.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import com.chatapp.chat_service.conversation.entity.InvitationLink;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationLinkRepository extends CassandraRepository<InvitationLink, UUID> {
    
    @Query("SELECT * FROM invitation_links WHERE link_token = ?0 ALLOW FILTERING")
    Optional<InvitationLink> findByLinkToken(String linkToken);
    
    @Query("SELECT * FROM invitation_links WHERE conversation_id = ?0 ALLOW FILTERING")
    List<InvitationLink> findByConversationId(UUID conversationId);
    
    @Query("SELECT * FROM invitation_links WHERE conversation_id = ?0 AND is_active = true ALLOW FILTERING")
    List<InvitationLink> findActiveByConversationId(UUID conversationId);
    
    @Query("SELECT * FROM invitation_links WHERE conversation_id = ?0 AND created_by = ?1 ALLOW FILTERING")
    List<InvitationLink> findByConversationIdAndCreatedBy(UUID conversationId, UUID createdBy);
    
    @Query("SELECT * FROM invitation_links WHERE conversation_id = ?0 AND is_active = true AND expires_at > ?1 ALLOW FILTERING")
    List<InvitationLink> findValidLinksByConversationId(UUID conversationId, Instant now);
}
