package com.chatapp.chat_service.message.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.chatapp.chat_service.message.entity.Message;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends CassandraRepository<Message, Message.MessageKey> {

    // 🔥 Query để lấy tin nhắn mới nhất với LIMIT trực tiếp (không dùng Pageable vì custom @Query không hỗ trợ tốt)
    @Query("SELECT * FROM messages_by_conversation WHERE conversation_id = ?0 ORDER BY message_id DESC LIMIT ?1")
    List<Message> findByConversationIdWithLimit(UUID conversationId, int limit);

    // 🔥 Derived query method (không có @Query) - Spring Data sẽ tự động tạo query và hỗ trợ Pageable
    List<Message> findByKeyConversationIdOrderByKeyMessageIdDesc(UUID conversationId, Pageable pageable);

    // 🔥 Query để lấy tin nhắn cũ hơn một message_id nhất định (sử dụng TIMEUUID comparison)
    @Query("SELECT * FROM messages_by_conversation WHERE conversation_id = ?0 AND message_id < ?1 ORDER BY message_id DESC LIMIT 30")
    List<Message> findOlderMessages(UUID conversationId, UUID beforeMessageId);

    @Query("SELECT * FROM messages_by_conversation WHERE conversation_id = ?0 AND message_id = ?1")
    Optional<Message> findByConversationIdAndMessageId(UUID conversationId, UUID messageId);

    @Query("SELECT * FROM messages_by_conversation WHERE conversation_id = ?0 AND created_at < ?1 ALLOW FILTERING")
    List<Message> findByConversationIdAndTimestampBefore(UUID conversationId, Instant before, Pageable pageable);

    @Query("SELECT * FROM messages_by_conversation WHERE conversation_id = ?0 AND created_at > ?1 ALLOW FILTERING")
    List<Message> findByConversationIdAndTimestampAfter(UUID conversationId, Instant after, Pageable pageable);

    @Query("SELECT * FROM messages_by_conversation WHERE conversation_id = ?0 AND created_at > ?1 AND created_at < ?2 ALLOW FILTERING")
    List<Message> findByConversationIdAndTimestampBetween(UUID conversationId, Instant after, Instant before, Pageable pageable);
}