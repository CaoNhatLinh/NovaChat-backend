package com.chatapp.chat_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("message_reactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReaction {

    @PrimaryKey
    private MessageReactionKey key;

    @Column("reacted_at")
    private Instant reactedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageReactionKey {
        @Column("conversation_id")
        private java.util.UUID conversationId;

        @Column("message_id")
        private java.util.UUID messageId;

        @Column("emoji")
        private String emoji; // "like", "love", "laugh", "angry", "sad", "wow"

        @Column("user_id")
        private java.util.UUID userId;
    }
}
