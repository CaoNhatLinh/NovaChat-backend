package com.chatapp.chat_service.security.interceptor;


import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.chatapp.chat_service.security.jwt.JwtService;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_ATTR = "USER_ID";
    private static final String USERNAME_ATTR = "USERNAME";
    
    private final JwtService jwtService;
    
    // Store authentication info by session ID
    private final ConcurrentHashMap<String, UserPrincipal> sessionAuth = new ConcurrentHashMap<>();

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        System.out.println("WebSocketAuthInterceptor preSend called");
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        
        System.out.println("STOMP Command: " + accessor.getCommand());
        System.out.println("Session ID: " + accessor.getSessionId());
        System.out.println("Current User: " + accessor.getUser());

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("Processing CONNECT command");
            String token = extractToken(accessor);

            if (token == null) {
                System.err.println("No authorization token found in headers");
                throw new AuthenticationCredentialsNotFoundException("Authorization header is missing");
            }

            System.out.println("Token found, validating...");
            if (!jwtService.validateToken(token)) {
                System.err.println("Token validation failed");
                throw new AuthenticationCredentialsNotFoundException("Invalid token");
            }

            System.out.println("Token is valid, authenticating user...");
            UserPrincipal userPrincipal = authenticateUser(accessor, token);
            
            // Store authentication in session map
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                sessionAuth.put(sessionId, userPrincipal);
                System.out.println("Stored authentication for session: " + sessionId);
            }
            
            System.out.println("User authenticated successfully. User: " + accessor.getUser());
            
        } else if (StompCommand.SEND.equals(accessor.getCommand()) || 
                   StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                   StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
            // NEW APPROACH: Extract token directly from message headers
            String token = extractToken(accessor);
            if (token != null && jwtService.validateToken(token)) {
                // Validate and extract user info from token directly
                String username = jwtService.extractUsername(token);
                UUID userId = jwtService.getUserIdFromToken(token);
                
                UserPrincipal userPrincipal = new UserPrincipal(userId, username);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        Collections.emptyList()
                );
                accessor.setUser(auth);
                
                // Set headers for controller fallback
                accessor.setNativeHeader("X-User-Id", userId.toString());
                accessor.setNativeHeader("X-Username", username);
                
                System.out.println("Direct token authentication for message - user: " + userId);
            } else {
                // Fallback to session-based auth
                String sessionId = accessor.getSessionId();
                if (sessionId != null) {
                    UserPrincipal storedAuth = sessionAuth.get(sessionId);
                    if (storedAuth != null) {
                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                storedAuth,
                                null,
                                Collections.emptyList()
                        );
                        accessor.setUser(auth);
                        
                        accessor.setNativeHeader("X-User-Id", storedAuth.getUserId().toString());
                        accessor.setNativeHeader("X-Username", storedAuth.getName());
                        
                        System.out.println("Session-based authentication for message - user: " + storedAuth.getUserId());
                    } else {
                        System.err.println("No authentication found - no token and no session data");
                        System.err.println("Available sessions: " + sessionAuth.keySet());
                    }
                } else {
                    System.err.println("No session ID found for non-CONNECT command");
                }
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            // Clean up session authentication
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                UserPrincipal removed = sessionAuth.remove(sessionId);
                if (removed != null) {
                    System.out.println("Removed authentication for disconnected session: " + sessionId);
                }
            }
        }
//
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authHeaders.get(0);
        if (authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private UserPrincipal authenticateUser(StompHeaderAccessor accessor, String token) {
        String username = jwtService.extractUsername(token);
        UUID userId = jwtService.getUserIdFromToken(token);
        
        System.out.println("Authenticating user - ID: " + userId + ", Username: " + username);

        // Tạo custom principal chứa cả userId
        UserPrincipal principal = new UserPrincipal(userId, username);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.emptyList()
        );

        accessor.setUser(auth);
        System.out.println("User authentication completed");
        return principal;
    }

    // Lớp custom principal để lưu thông tin người dùng
    public static class UserPrincipal implements Principal {
        private final UUID userId;
        private final String username;

        public UserPrincipal(UUID userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public UUID getUserId() {
            return userId;
        }

        @Override
        public String getName() {
            return username;
        }
    }
}