package com.chatapp.chat_service.message.controller;

import com.chatapp.chat_service.common.dto.ApiResponse;
import com.chatapp.chat_service.message.dto.MessageAttachmentDto;
import com.chatapp.chat_service.message.dto.MessageReactionDto;
import com.chatapp.chat_service.message.dto.MessageResponseDto;
import com.chatapp.chat_service.message.entity.MessageReadReceipt;
import com.chatapp.chat_service.message.entity.PinnedMessage;
import com.chatapp.chat_service.message.service.MessageEnhancementService;
import com.chatapp.chat_service.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MessageEnhancementService enhancementService;

    /**
     * Lấy tin nhắn mới nhất của conversation
     * GET /api/messages/{conversationId}?size=20&page=0
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<List<MessageResponseDto>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // Max 100 per request
        return ResponseEntity.ok(
                messageService.getLatestMessages(conversationId, pageable)
        );
    }

    /**
     * Lấy tin nhắn mới nhất của conversation (API đơn giản)
     * GET /api/messages/conversations/{conversationId}?limit=20
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> getConversationMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 100));
        List<MessageResponseDto> messages = messageService.getLatestMessages(conversationId, pageable);
        
        ApiResponse<List<MessageResponseDto>> response = ApiResponse.success(
            "Get messages of conversation successfully", 
            messages
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tin nhắn cũ hơn một message nhất định (pagination thủ công)
     * GET /api/messages/conversations/{conversationId}/older?beforeMessageId={messageId}
     */
    @GetMapping("/conversations/{conversationId}/older")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> getOlderMessages(
            @PathVariable UUID conversationId,
            @RequestParam UUID beforeMessageId
    ) {
        List<MessageResponseDto> messages = messageService.getOlderMessages(conversationId, beforeMessageId);
        
        ApiResponse<List<MessageResponseDto>> response = ApiResponse.success(
            "Get older messages successfully", 
            messages
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tin nhắn với filter thời gian
     * GET /api/messages/conversations/{conversationId}/filtered?before=2024-01-01T10:00:00&after=2024-01-01T09:00:00&size=20
     */
    @GetMapping("/conversations/{conversationId}/filtered")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> getFilteredMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        List<MessageResponseDto> messages = messageService.getConversationMessages(conversationId, before, after, pageable);
        
        ApiResponse<List<MessageResponseDto>> response = ApiResponse.success(
            "Get filtered messages successfully", 
            messages
        );
        
        return ResponseEntity.ok(response);
    }

    // ==================== ATTACHMENT ENDPOINTS ====================

    @PostMapping("/{conversationId}/{messageId}/attachments")
    public ResponseEntity<MessageAttachmentDto> addAttachment(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @RequestBody MessageAttachmentDto attachmentDto
    ) {
        MessageAttachmentDto result = enhancementService.addAttachment(conversationId, messageId, attachmentDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{conversationId}/{messageId}/attachments")
    public ResponseEntity<List<MessageAttachmentDto>> getMessageAttachments(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId
    ) {
        List<MessageAttachmentDto> attachments = enhancementService.getMessageAttachments(conversationId, messageId);
        return ResponseEntity.ok(attachments);
    }

    // ==================== REACTION ENDPOINTS ====================

    @PostMapping("/{conversationId}/{messageId}/reactions/{emoji}")
    public ResponseEntity<Void> toggleReaction(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @PathVariable String emoji,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        enhancementService.toggleReaction(conversationId, messageId, emoji, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{conversationId}/{messageId}/reactions")
    public ResponseEntity<List<MessageReactionDto>> getMessageReactions(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        List<MessageReactionDto> reactions = enhancementService.getMessageReactions(conversationId, messageId, currentUserId);
        return ResponseEntity.ok(reactions);
    }

    // ==================== READ RECEIPT ENDPOINTS ====================

    @PostMapping("/{conversationId}/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        UUID readerId = UUID.fromString(authentication.getName());
        enhancementService.markAsRead(conversationId, messageId, readerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{conversationId}/{messageId}/read-receipts")
    public ResponseEntity<List<MessageReadReceipt>> getReadReceipts(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId
    ) {
        List<MessageReadReceipt> receipts = enhancementService.getMessageReadReceipts(conversationId, messageId);
        return ResponseEntity.ok(receipts);
    }

    // ==================== PINNED MESSAGE ENDPOINTS ====================

    @PostMapping("/{conversationId}/{messageId}/pin")
    public ResponseEntity<Void> togglePinMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            Authentication authentication
    ) {
        UUID pinnedBy = UUID.fromString(authentication.getName());
        enhancementService.togglePinMessage(conversationId, messageId, pinnedBy);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{conversationId}/pinned")
    public ResponseEntity<List<PinnedMessage>> getPinnedMessages(
            @PathVariable UUID conversationId
    ) {
        List<PinnedMessage> pinnedMessages = enhancementService.getPinnedMessages(conversationId);
        return ResponseEntity.ok(pinnedMessages);
    }

    /**
     * Test endpoint để debug pagination issue
     * GET /api/messages/conversations/{conversationId}/debug?limit=20&method=custom
     */
    @GetMapping("/conversations/{conversationId}/debug")
    public ResponseEntity<ApiResponse<Object>> debugMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "custom") String method
    ) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 100));
        
        List<MessageResponseDto> messages;
        String methodUsed;
        
        if ("derived".equals(method)) {
            messages = messageService.getLatestMessagesAlternative(conversationId, pageable);
            methodUsed = "Derived Query Method";
        } else {
            messages = messageService.getLatestMessages(conversationId, pageable);
            methodUsed = "Custom @Query with LIMIT";
        }
        
        Object debugInfo = Map.of(
            "method", methodUsed,
            "requestedLimit", limit,
            "actualCount", messages.size(),
            "messages", messages
        );
        
        ApiResponse<Object> response = ApiResponse.success(
            "Debug messages - " + methodUsed, 
            debugInfo
        );
        
        return ResponseEntity.ok(response);
    }
}