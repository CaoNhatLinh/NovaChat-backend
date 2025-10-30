package com.chatapp.chat_service.poll.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table("poll_votes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {

    @PrimaryKey
    private PollVoteKey key;

    @Column("selected_options")
    private List<String> selectedOptions; // Support multiple choice

    @Column("voted_at")
    private Instant votedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PollVoteKey {
        @Column("poll_id")
        private UUID pollId;

        @Column("user_id")
        private UUID userId;
    }
}
