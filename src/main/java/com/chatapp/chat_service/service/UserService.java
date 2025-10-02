package com.chatapp.chat_service.service;

import com.chatapp.chat_service.model.dto.UserDTO;
import com.chatapp.chat_service.model.dto.ConversationResponseDto;
import com.chatapp.chat_service.model.entity.User;
import com.chatapp.chat_service.repository.UserRepository;
import com.chatapp.chat_service.service.presence.OnlineStatusService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final OnlineStatusService onlineStatusService;

    public UserService(UserRepository userRepository, OnlineStatusService onlineStatusService) {
        this.userRepository = userRepository;
        this.onlineStatusService = onlineStatusService;
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
        return onlineStatusService.isUserOnline(userId);
    }

}
