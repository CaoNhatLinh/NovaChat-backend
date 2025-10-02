package com.chatapp.chat_service.model.dto;



import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String username

) {
    public AuthResponse withoutToken() {
        return new AuthResponse(
                null, // token removed
                userId,
                username
        );
    }
}