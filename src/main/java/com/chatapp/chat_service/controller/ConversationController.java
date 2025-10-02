package com.chatapp.chat_service.controller;

import com.chatapp.chat_service.model.dto.ConversationRequest;
import com.chatapp.chat_service.model.dto.ConversationResponseDto;
import com.chatapp.chat_service.model.dto.ConversationSearchDto;
import com.chatapp.chat_service.model.entity.Conversation;
import com.chatapp.chat_service.model.entity.CustomUserDetails;
import com.chatapp.chat_service.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/my")
    public ResponseEntity<List<ConversationResponseDto>> getMyConversations(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        List<ConversationResponseDto> conversations = conversationService.getUserConversationsWithDetails(userId);
        return ResponseEntity.ok(conversations);
    }
    @PostMapping("/create")
    public ResponseEntity<Conversation> createConversation(@RequestBody ConversationRequest conversationRequest, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        Conversation createdConversation = conversationService.createConversation(conversationRequest, userId);
        return ResponseEntity.ok(createdConversation);
    }
    /**
     * Tìm phòng chat private giữa 2 user (chỉ trả về phòng chưa bị xóa)
     * URL: GET /api/conversations/dm?userId1={userId1}&userId2={userId2}
     */
    @GetMapping("/dm")
    public ResponseEntity<Conversation> findPrivateConversation(
            @RequestParam UUID userId1,
            @RequestParam UUID userId2
    ) {
        // Sử dụng phương thức có cache và đảm bảo chỉ trả về conversation chưa bị xóa
        Optional<Conversation> conversation = conversationService.findPrivateConversationWithCache(userId1, userId2);

        if (conversation.isPresent()) {
            return ResponseEntity.ok(conversation.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Soft delete một conversation
     * URL: DELETE /api/conversations/{conversationId}
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<String> deleteConversation(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();
            
            boolean deleted = conversationService.deleteConversation(conversationId, userId);
            
            if (deleted) {
                return ResponseEntity.ok("Conversation đã được xóa thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Khôi phục một conversation đã bị soft delete
     * URL: PUT /api/conversations/{conversationId}/restore
     */
    @PutMapping("/{conversationId}/restore")
    public ResponseEntity<String> restoreConversation(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UUID userId = userDetails.getUserId();
            
            boolean restored = conversationService.restoreConversation(conversationId, userId);
            
            if (restored) {
                return ResponseEntity.ok("Conversation đã được khôi phục thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Tìm kiếm các cuộc trò chuyện
     * URL: GET /api/conversations/search?name={name}&type={type}
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ConversationSearchDto>> searchConversations(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            Pageable pageable,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        Page<ConversationSearchDto> results = conversationService.searchConversations(userId, name, type, pageable);
        return ResponseEntity.ok(results);
    }

}