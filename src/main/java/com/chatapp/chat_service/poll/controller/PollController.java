package com.chatapp.chat_service.poll.controller;

import com.chatapp.chat_service.poll.dto.PollDto;
import com.chatapp.chat_service.poll.entity.Poll;
import com.chatapp.chat_service.poll.service.MessagePollService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final MessagePollService pollService;

    /**
     * Tạo poll mới
     */
    @PostMapping
    public ResponseEntity<Poll> createPoll(
            @RequestParam UUID conversationId,
            @RequestParam UUID messageId,
            @RequestParam String question,
            @RequestParam List<String> options,
            @RequestParam(defaultValue = "false") boolean isMultipleChoice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant expiresAt,
            Authentication authentication
    ) {
        UUID createdBy = UUID.fromString(authentication.getName());
        Poll poll = pollService.createPoll(conversationId, messageId, question, options, createdBy, isMultipleChoice, expiresAt);
        return ResponseEntity.ok(poll);
    }

    /**
     * Vote trong poll
     */
    @PostMapping("/{pollId}/vote")
    public ResponseEntity<Void> vote(
            @PathVariable UUID pollId,
            @RequestParam List<String> selectedOptions,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        pollService.vote(pollId, userId, selectedOptions);
        return ResponseEntity.ok().build();
    }

    /**
     * Lấy kết quả poll
     */
    @GetMapping("/{pollId}/results")
    public ResponseEntity<PollDto> getPollResults(
            @PathVariable UUID pollId,
            Authentication authentication
    ) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        PollDto results = pollService.getPollResults(pollId, currentUserId);
        return ResponseEntity.ok(results);
    }

    /**
     * Đóng poll
     */
    @PostMapping("/{pollId}/close")
    public ResponseEntity<Void> closePoll(
            @PathVariable UUID pollId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        pollService.closePoll(pollId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Xóa vote
     */
    @DeleteMapping("/{pollId}/vote")
    public ResponseEntity<Void> removeVote(
            @PathVariable UUID pollId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        pollService.removeVote(pollId, userId);
        return ResponseEntity.ok().build();
    }
}
