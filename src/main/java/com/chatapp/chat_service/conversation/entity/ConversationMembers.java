package com.chatapp.chat_service.conversation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Table("conversation_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMembers {
    @PrimaryKey
    private ConversationMemberKey key;
    private Instant joined_at;
    private String role;

    @PrimaryKeyClass
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMemberKey implements Serializable {
        @PrimaryKeyColumn(name = "conversation_id", type = PrimaryKeyType.PARTITIONED)
        private UUID conversationId;

        @PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.CLUSTERED)
        private UUID userId;

        // Implement equals() and hashCode() for composite key
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConversationMemberKey that = (ConversationMemberKey) o;
            return Objects.equals(conversationId, that.conversationId) &&
                    Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conversationId, userId);
        }
    }

    // Convenience constructor
    public ConversationMembers(UUID conversationId, UUID userId, Instant joined_at, String role) {
        this.key = new ConversationMemberKey(conversationId, userId);
        this.joined_at = joined_at;
        this.role = role;
    }

    // Helper methods for easier access
    public UUID getUserId() {
        return key != null ? key.getUserId() : null;
    }

    public UUID getConversationId() {
        return key != null ? key.getConversationId() : null;
    }
}