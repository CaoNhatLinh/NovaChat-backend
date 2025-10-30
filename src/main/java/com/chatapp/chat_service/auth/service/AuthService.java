package com.chatapp.chat_service.auth.service;

import com.chatapp.chat_service.auth.dto.AuthResponse;
import com.chatapp.chat_service.auth.dto.LoginRequest;
import com.chatapp.chat_service.auth.dto.RegisterRequest;
import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.auth.entity.User;
import com.chatapp.chat_service.auth.repository.UserRepository;
import com.chatapp.chat_service.security.core.CustomUserDetails;
import com.chatapp.chat_service.security.jwt.JwtService;
import com.chatapp.chat_service.security.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



import java.time.Duration;

import java.util.*;

@Service

@RequiredArgsConstructor
public class AuthService  {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwUtil;

    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        System.out.println("🔍 AuthService: login - Authentication successful for user: " + request.getUsername());
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(customUserDetails.getUsername(), customUserDetails.getUserId());

        String key = "user_presence:" + customUserDetails.getUserId();
        Map<String, Object> presence = new HashMap<>();
        presence.put("status", "online");
        presence.put("last_seen", null);
        redisTemplate.opsForHash().putAll(key, presence);
        redisTemplate.expire(key, Duration.ofHours(12)); // auto expire nếu quên logout

        return new AuthResponse(token, customUserDetails.getUserId(), customUserDetails.getUsername());
    }

    //logout method
    public void logout(String token) {
        String username = jwtService.extractUsername(token);
        Optional<User> user = userRepository.findFirstByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        // Cập nhật trạng thái offline trong Redis
        String key = "user_presence:" + user.get().getUser_id();
        Map<String, Object> presence = new HashMap<>();
        presence.put("status", "offline");
        presence.put("last_seen", System.currentTimeMillis());
        redisTemplate.opsForHash().putAll(key, presence);
    }
    // register method
    public User register(RegisterRequest RegisterRequest) {

        System.out.println("🔍 AuthService: register - Checking if username exists: " + RegisterRequest.getUsername());
        if (userRepository.existsByUsername(RegisterRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        System.out.println("🔍 AuthService: register - Registering user: " + RegisterRequest.getUsername());
        User user = new User();
        user.setUser_id(UUID.randomUUID());
        user.setUsername(RegisterRequest.getUsername());
        String hashedPassword = passwordEncoder.encode(RegisterRequest.getPassword());
        user.setPassword(hashedPassword);

        user.setDisplay_name(RegisterRequest.getDisplay_name());

        return userRepository.save(user);
    }

    public UserDTO getCurrentUser(String username) {
        Optional<User> user = userRepository.findFirstByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return UserDTO.builder()
                .user_id(user.get().getUser_id())
                .username(user.get().getUsername())
                .display_name(user.get().getDisplay_name())
                .avatar_url(user.get().getAvatar_url())
                .build();
    }
}
