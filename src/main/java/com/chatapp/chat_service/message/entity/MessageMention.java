package com.chatapp.chat_service.message.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("message_mentions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMention {

    @PrimaryKey
    private MessageMentionKey key;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageMentionKey {
        @Column("conversation_id")
        private java.util.UUID conversationId;

        @Column("message_id")
        private java.util.UUID messageId;

        @Column("mentioned_user_id")
        private java.util.UUID mentionedUserId;
    }
}
