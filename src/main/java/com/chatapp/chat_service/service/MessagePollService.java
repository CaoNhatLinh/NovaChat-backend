package com.chatapp.chat_service.service;

import com.chatapp.chat_service.model.dto.PollDto;
import com.chatapp.chat_service.model.entity.Poll;
import com.chatapp.chat_service.model.entity.PollVote;
import com.chatapp.chat_service.repository.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePollService {

    private final PollVoteRepository pollVoteRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Tạo poll mới
     */
    public Poll createPoll(UUID conversationId, UUID messageId, String question, List<String> options, 
                          UUID createdBy, boolean isMultipleChoice, Instant expiresAt) {
        Poll poll = Poll.builder()
                .pollId(UUID.randomUUID())
                .conversationId(conversationId)
                .messageId(messageId)
                .question(question)
                .options(options)
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .isClosed(false)
                .isMultipleChoice(isMultipleChoice)
                .expiresAt(expiresAt)
                .build();

        // Save to database would be here if we had pollRepository
        
        // Cache poll
        String cacheKey = "poll:" + poll.getPollId();
        redisTemplate.opsForValue().set(cacheKey, poll, Duration.ofHours(24));

        log.info("Created poll {} in conversation {}", poll.getPollId(), conversationId);
        return poll;
    }

    /**
     * Vote trong poll
     */
    public void vote(UUID pollId, UUID userId, List<String> selectedOptions) {
        // Get poll info (would query database in real implementation)
        String pollCacheKey = "poll:" + pollId;
        Poll poll = (Poll) redisTemplate.opsForValue().get(pollCacheKey);
        
        if (poll == null) {
            throw new IllegalArgumentException("Poll not found");
        }

        if (poll.getIsClosed()) {
            throw new IllegalStateException("Poll is closed");
        }

        if (poll.getExpiresAt() != null && Instant.now().isAfter(poll.getExpiresAt())) {
            throw new IllegalStateException("Poll has expired");
        }

        if (!poll.getIsMultipleChoice() && selectedOptions.size() > 1) {
            throw new IllegalArgumentException("Multiple selection not allowed");
        }

        // Validate options
        for (String option : selectedOptions) {
            if (!poll.getOptions().contains(option)) {
                throw new IllegalArgumentException("Invalid option: " + option);
            }
        }

        PollVote.PollVoteKey voteKey = new PollVote.PollVoteKey(pollId, userId);
        
        // Remove existing vote if any
        pollVoteRepository.deleteByPollIdAndUserId(pollId, userId);

        // Add new vote
        PollVote vote = PollVote.builder()
                .key(voteKey)
                .selectedOptions(selectedOptions)
                .votedAt(Instant.now())
                .build();

        pollVoteRepository.save(vote);

        // Clear cache
        clearPollResultsCache(pollId);

        // Send real-time update
        PollDto pollResults = getPollResults(pollId, userId);
        messagingTemplate.convertAndSend("/topic/conversation/" + poll.getConversationId() + "/polls", pollResults);

        log.info("User {} voted in poll {} with options {}", userId, pollId, selectedOptions);
    }

    /**
     * Lấy kết quả poll
     */
    public PollDto getPollResults(UUID pollId, UUID currentUserId) {
        String cacheKey = "poll_results:" + pollId;
        
        // Try cache first
        PollDto cachedResults = (PollDto) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResults != null) {
            return cachedResults;
        }

        // Get poll info (would query database in real implementation)
        String pollCacheKey = "poll:" + pollId;
        Poll poll = (Poll) redisTemplate.opsForValue().get(pollCacheKey);
        
        if (poll == null) {
            throw new IllegalArgumentException("Poll not found");
        }

        // Get all votes
        List<PollVote> votes = pollVoteRepository.findByPollId(pollId);
        long totalVotes = votes.size();

        // Get current user's votes
        List<String> currentUserVotes = votes.stream()
                .filter(vote -> vote.getKey().getUserId().equals(currentUserId))
                .flatMap(vote -> vote.getSelectedOptions().stream())
                .collect(Collectors.toList());

        // Calculate results for each option
        List<PollDto.PollOptionDto> optionResults = poll.getOptions().stream()
                .map(option -> {
                    List<UUID> voterIds = votes.stream()
                            .filter(vote -> vote.getSelectedOptions().contains(option))
                            .map(vote -> vote.getKey().getUserId())
                            .collect(Collectors.toList());
                    
                    long voteCount = voterIds.size();
                    double percentage = totalVotes > 0 ? (double) voteCount / totalVotes * 100 : 0;

                    return PollDto.PollOptionDto.builder()
                            .option(option)
                            .voteCount(voteCount)
                            .percentage(percentage)
                            .voterIds(voterIds)
                            .build();
                })
                .collect(Collectors.toList());

        PollDto results = PollDto.builder()
                .pollId(poll.getPollId())
                .conversationId(poll.getConversationId())
                .messageId(poll.getMessageId())
                .question(poll.getQuestion())
                .options(optionResults)
                .isClosed(poll.getIsClosed())
                .isMultipleChoice(poll.getIsMultipleChoice())
                .createdBy(poll.getCreatedBy())
                .createdAt(poll.getCreatedAt())
                .expiresAt(poll.getExpiresAt())
                .totalVotes(totalVotes)
                .currentUserVotes(currentUserVotes)
                .build();

        // Cache results
        redisTemplate.opsForValue().set(cacheKey, results, Duration.ofMinutes(5));

        return results;
    }

    /**
     * Đóng poll
     */
    public void closePoll(UUID pollId, UUID userId) {
        String pollCacheKey = "poll:" + pollId;
        Poll poll = (Poll) redisTemplate.opsForValue().get(pollCacheKey);
        
        if (poll == null) {
            throw new IllegalArgumentException("Poll not found");
        }

        if (!poll.getCreatedBy().equals(userId)) {
            throw new IllegalStateException("Only poll creator can close the poll");
        }

        poll.setIsClosed(true);
        
        // Update cache
        redisTemplate.opsForValue().set(pollCacheKey, poll, Duration.ofHours(24));
        
        // Clear results cache to force refresh
        clearPollResultsCache(pollId);

        // Send real-time update
        PollDto pollResults = getPollResults(pollId, userId);
        messagingTemplate.convertAndSend("/topic/conversation/" + poll.getConversationId() + "/polls", pollResults);

        log.info("Poll {} closed by user {}", pollId, userId);
    }

    /**
     * Xóa vote của user
     */
    public void removeVote(UUID pollId, UUID userId) {
        pollVoteRepository.deleteByPollIdAndUserId(pollId, userId);
        
        // Clear cache
        clearPollResultsCache(pollId);

        // Get poll info for real-time update
        String pollCacheKey = "poll:" + pollId;
        Poll poll = (Poll) redisTemplate.opsForValue().get(pollCacheKey);
        
        if (poll != null) {
            PollDto pollResults = getPollResults(pollId, userId);
            messagingTemplate.convertAndSend("/topic/conversation/" + poll.getConversationId() + "/polls", pollResults);
        }

        log.info("Removed vote for user {} in poll {}", userId, pollId);
    }

    private void clearPollResultsCache(UUID pollId) {
        String cacheKey = "poll_results:" + pollId;
        redisTemplate.delete(cacheKey);
    }
}
