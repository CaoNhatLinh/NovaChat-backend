package com.chatapp.chat_service.repository;

import com.chatapp.chat_service.model.entity.PinnedMessage;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PinnedMessageRepository extends CassandraRepository<PinnedMessage, PinnedMessage.PinnedMessageKey> {

    @Query("SELECT * FROM pinned_messages WHERE conversation_id = ?0")
    List<PinnedMessage> findByConversationId(UUID conversationId);

    @Query("SELECT COUNT(*) FROM pinned_messages WHERE conversation_id = ?0")
    long countByConversationId(UUID conversationId);

    @Query("DELETE FROM pinned_messages WHERE conversation_id = ?0 AND message_id = ?1")
    void deleteByConversationIdAndMessageId(UUID conversationId, UUID messageId);
}
