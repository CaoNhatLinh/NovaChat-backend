package com.chatapp.chat_service.repository;

import com.chatapp.chat_service.model.entity.Conversation;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends CassandraRepository<Conversation, UUID> {

    Optional<Conversation> findByConversationId(UUID id);

}
