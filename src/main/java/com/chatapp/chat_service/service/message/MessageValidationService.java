package com.chatapp.chat_service.service.message;

import com.chatapp.chat_service.exception.ForbiddenException;
import com.chatapp.chat_service.repository.ConversationMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service validation cho message operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageValidationService {
    
    private final ConversationMemberRepository conversationMemberRepository;
    
    /**
     * Kiểm tra user có phải member của conversation không
     * @param conversationId ID của conversation
     * @param userId ID của user
     * @throws ForbiddenException nếu user không phải member
     */
    public void validateConversationMembership(UUID conversationId, UUID userId) {
        if (!conversationMemberRepository.existsByKeyConversationIdAndKeyUserId(conversationId, userId)) {
            throw new ForbiddenException("You are not a member of this conversation");
        }
    }
    
    /**
     * Kiểm tra user có quyền gửi message không
     * @param conversationId ID của conversation
     * @param userId ID của user
     * @throws ForbiddenException nếu user không có quyền
     */
    public void validateMessagePermission(UUID conversationId, UUID userId) {
        validateConversationMembership(conversationId, userId);
        // Có thể thêm các validation khác như:
        // - Kiểm tra conversation có bị mute không
        // - Kiểm tra user có bị block không
        // - Kiểm tra conversation có bị archive không
    }
}
