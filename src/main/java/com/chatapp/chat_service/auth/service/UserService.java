package com.chatapp.chat_service.auth.service;

import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.auth.entity.User;
import com.chatapp.chat_service.auth.repository.UserRepository;
import com.chatapp.chat_service.conversation.dto.ConversationResponseDto;
import com.chatapp.chat_service.presence.service.PresenceService;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PresenceService presenceService;

    public UserService(UserRepository userRepository, PresenceService presenceService) {
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }


    public UserDTO findByUsername(String query) {
        return userRepository.findFirstByUsername(query)
                .map(user -> UserDTO.builder()
                        .user_id(user.getUser_id())
                        .username(user.getUsername())
                        .display_name(user.getDisplay_name())
                        .nickname(user.getNickname())
                        .avatar_url(user.getAvatar_url())
                        .build())
                .orElse(null);
    }

    private UserDTO convertToDto(User user) {
        return UserDTO.builder()
                .user_id(user.getUser_id())
                .username(user.getUsername())
                .display_name(user.getDisplay_name())
                .nickname(user.getNickname())
                .avatar_url(user.getAvatar_url())
                .build();
    }
    public User save(User user) {
        return userRepository.save(user);
    }

    // Các hàm tiện ích khác như update displayName, avatar, etc.
    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId);
    }

    /**
     * Lấy thông tin user profile cho conversation
     */
    public ConversationResponseDto.UserProfileDto getUserProfile(UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ConversationResponseDto.UserProfileDto.builder()
                    .userId(user.getUser_id())
                    .username(user.getUsername())
                    .displayName(user.getDisplay_name())
                    .email(null) // User entity doesn't have email field
                    .avatarUrl(user.getAvatar_url())
                    .isOnline(isUserOnline(userId))
                    .build();
        }
        
        // Return default if user not found
        return ConversationResponseDto.UserProfileDto.builder()
                .userId(userId)
                .username("Unknown User")
                .displayName("Unknown User")
                .email(null)
                .avatarUrl(null)
                .isOnline(false)
                .build();
    }
    
    /**
     * Kiểm tra user có online không
     */
    public boolean isUserOnline(UUID userId) {
        return presenceService.isUserOnline(userId);
    }

}
