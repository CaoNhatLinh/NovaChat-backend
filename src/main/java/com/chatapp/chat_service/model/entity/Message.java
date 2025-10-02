package com.chatapp.chat_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("messages_by_conversation")
public class Message {

    @PrimaryKey
    private MessageKey key;

    @Column("sender_id")
    private UUID senderId;
    private String content;
    @Column("created_at")
    private Instant createdAt;
    @Column("edited_at")
    private Instant editedAt;
    private String type; // "message", "image", "file", etc.
    @Column("is_deleted")
    private boolean isDeleted;
    @Column("reply_to")
    private UUID replyTo;

    @Column("mentioned_user_ids")
    private List<UUID> mentionedUserIds;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @PrimaryKeyClass
    public static class MessageKey {
        @PrimaryKeyColumn(name = "conversation_id", type = PrimaryKeyType.PARTITIONED)
        private UUID conversationId;

        @PrimaryKeyColumn(name = "message_id", type = PrimaryKeyType.CLUSTERED)
        private UUID messageId;
    }
}