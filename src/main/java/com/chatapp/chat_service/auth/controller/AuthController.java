package com.chatapp.chat_service.auth.controller;

import com.chatapp.chat_service.auth.dto.AuthResponse;
import com.chatapp.chat_service.auth.dto.LoginRequest;
import com.chatapp.chat_service.auth.dto.RegisterRequest;
import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.auth.entity.User;
import com.chatapp.chat_service.auth.service.AuthService;
import com.chatapp.chat_service.security.core.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok().build();
    }
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest RegisterRequest) {
        User savedUser = authService.register(RegisterRequest);
        return ResponseEntity.ok(savedUser);
    }
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }
}

