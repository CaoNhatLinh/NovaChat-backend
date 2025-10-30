package com.chatapp.chat_service.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollDto {
    private UUID pollId;
    private UUID conversationId;
    private UUID messageId;
    private String question;
    private List<PollOptionDto> options;
    private boolean isClosed;
    private boolean isMultipleChoice;
    private UUID createdBy;
    private String createdByUsername;
    private Instant createdAt;
    private Instant expiresAt;
    private long totalVotes;
    private List<String> currentUserVotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PollOptionDto {
        private String option;
        private long voteCount;
        private double percentage;
        private List<UUID> voterIds;
    }
}
