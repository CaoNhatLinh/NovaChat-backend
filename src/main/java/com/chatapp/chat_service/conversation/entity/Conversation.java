package com.chatapp.chat_service.conversation.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import com.chatapp.chat_service.message.dto.MessageSummary;

import java.time.Instant;
import java.util.UUID  ;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("conversations")
public class Conversation {
    @PrimaryKey("conversation_id")
    private UUID conversationId;
    private String type; // 'group' or 'dm'
    private String name;
    private String description;
    private boolean is_deleted;
    private MessageSummary last_message;
    private UUID created_by;
    private String background_url;
    private Instant created_at;
    private Instant updated_at;
}
