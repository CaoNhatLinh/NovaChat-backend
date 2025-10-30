package com.chatapp.chat_service.message.mapper;

import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.auth.service.UserService;
import com.chatapp.chat_service.message.dto.MessageResponse;
import com.chatapp.chat_service.message.dto.MessageResponseDto;
import com.chatapp.chat_service.message.dto.MessageSummary;
import com.chatapp.chat_service.message.entity.Message;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MessageMapper {
    
    private final UserService userService;
    
    public MessageMapper(UserService userService) {
        this.userService = userService;
    }
    
    public MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .messageId(message.getKey().getMessageId())
                .conversationId(message.getKey().getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .editedAt(message.getEditedAt())
                .isDeleted(message.isDeleted())
                .replyTo(message.getReplyTo())
                .mentionedUserIds(message.getMentionedUserIds())
                .build();
    }

    public MessageSummary toSummary(Message message) {
        return MessageSummary.builder()
                .messageId(message.getKey().getMessageId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public MessageResponseDto toResponseDto(Message message) {
        return MessageResponseDto.builder()
                .messageId(message.getKey().getMessageId())
                .conversationId(message.getKey().getConversationId())
                .content(message.getContent())
                .mentionedUsers(message.getMentionedUserIds() != null ?
                    message.getMentionedUserIds().stream().map(UUID::toString).collect(Collectors.toList()) : new ArrayList<>())
                .messageType(message.getType()) 
                .attachments(new ArrayList<>())
                .images(new ArrayList<>()) 
                .reactions(new ArrayList<>()) 
                .replyTo(null) 
                .replyType("Message")
                .isForwarded(false) 
                .isDeleted(message.isDeleted())
                .deletedAt(null) 
                .createdAt(LocalDateTime.ofInstant(message.getCreatedAt(), ZoneId.systemDefault()))
                .updatedAt(message.getEditedAt() != null ?
                    LocalDateTime.ofInstant(message.getEditedAt(), ZoneId.systemDefault()) : null)
                .sender(createSenderDto(message.getSenderId())) 
                .build();
    }

    private UserDTO createSenderDto(UUID senderId) {
        return userService.findById(senderId)
                .map(user -> UserDTO.builder()
                        .user_id(user.getUser_id())
                        .username(user.getUsername())
                        .display_name(user.getDisplay_name())
                        .nickname(user.getNickname())
                        .avatar_url(user.getAvatar_url())
                        .created_at(user.getCreated_at() != null ? user.getCreated_at().toString() : null)
                        .build())
                .orElse(UserDTO.builder()
                        .user_id(senderId)
                        .display_name("Unknown User")
                        .username("unknown_" + senderId.toString().substring(0, 8))
                        .avatar_url(null)
                        .build());
    }
}