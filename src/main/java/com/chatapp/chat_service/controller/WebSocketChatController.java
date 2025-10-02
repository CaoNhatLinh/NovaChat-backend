package com.chatapp.chat_service.controller;


import com.chatapp.chat_service.kafka.messaging.KafkaMessageProducer;
import com.chatapp.chat_service.model.dto.*;
import com.chatapp.chat_service.service.UserService;
import com.chatapp.chat_service.service.typing.TypingIndicatorService;
import com.chatapp.chat_service.service.presence.PresenceService;
import com.chatapp.chat_service.service.websocket.WebSocketConnectionService;
//import com.chatapp.chat_service.service.subscription.PresenceSubscriptionService;
import com.chatapp.chat_service.websocket.event.OnlineStatusEvent;
import com.chatapp.chat_service.websocket.event.TypingEvent;
import com.chatapp.chat_service.websocket.event.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.chatapp.chat_service.service.NotificationService;
import com.chatapp.chat_service.security.JwtService;
import com.datastax.oss.driver.api.core.uuid.Uuids;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final PresenceService presenceService;
    private final WebSocketConnectionService webSocketConnectionService;
//    private final PresenceSubscriptionService presenceSubscriptionService;
    private final KafkaMessageProducer kafkaProducer;
    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final TypingIndicatorService typingIndicatorService;


    // NEW: Handler cho file messages với attachments
    @MessageMapping("/message.file")
    public void handleFileMessage(@Payload MessageEvent event, 
                                 Principal principal,
                                 @Header(value = "Authorization", required = false) String authHeader) {
        try {
            UUID senderId = extractUserIdFromPrincipalOrToken(principal, authHeader);
            System.out.println("Processing file message from user: " + senderId);

            // 1. IMMEDIATE ECHO - Gửi phản hồi ngay lập tức cho người gửi
            UserDTO senderUser = userService.findById(senderId)
                    .map(user -> UserDTO.builder()
                            .user_id(user.getUser_id())
                            .username(user.getUsername())
                            .display_name(user.getDisplay_name())
                            .nickname(user.getNickname())
                            .avatar_url(user.getAvatar_url())
                            .created_at(user.getCreated_at() != null ? user.getCreated_at().toString() : null)
                            .build())
                    .orElse(null);

            // Convert file attachments to response format
            List<MessageResponseDto.FileAttachmentDto> fileAttachments = null;
            if (event.getPayload().getAttachments() != null) {
                fileAttachments = event.getPayload().getAttachments().stream()
                        .map(attachment -> MessageResponseDto.FileAttachmentDto.builder()
                                .url(attachment.getUrl())
                                .fileName(attachment.getFileName())
                                .contentType(attachment.getContentType())
                                .fileSize(attachment.getFileSize())
                                .resourceType(attachment.getResourceType())
                                .publicId(attachment.getPublicId())
                                .thumbnailUrl(attachment.getThumbnailUrl())
                                .mediumUrl(attachment.getMediumUrl())
                                .build())
                        .toList();
            }

            MessageResponseDto echoResponse = MessageResponseDto.builder()
                    .messageId(Uuids.timeBased())
                    .conversationId(event.getPayload().getConversationId())
                    .content(event.getPayload().getContent())
                    .sender(senderUser)
                    .messageType(event.getPayload().getType()) // FILE, IMAGE, VIDEO, AUDIO
                    .fileAttachments(fileAttachments)
                    .createdAt(java.time.LocalDateTime.now())
                    .isDeleted(false)
                    .isForwarded(false)
                    .build();

            // Gửi echo về sender ngay lập tức
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/message-echo",
                    echoResponse
            );

            // 2. ASYNC PROCESSING - Chuyển đổi sang MessageRequest để xử lý qua Kafka
            MessageRequest messageRequest = MessageRequest.builder()
                    .conversationId(event.getPayload().getConversationId())
                    .content(event.getPayload().getContent())
                    .type(event.getPayload().getType()) // FILE, IMAGE, VIDEO, AUDIO
                    .mentionedUserIds(event.getPayload().getMentions())
                    .replyTo(event.getPayload().getReplyTo())
                    .senderId(senderId)
                    .attachments(event.getPayload().getAttachments()) // File attachments
                    .build();

            // 3. GỬI VÀO KAFKA
            MessageEvent kafkaEvent = MessageEvent.forKafkaProcessing(messageRequest);
            kafkaProducer.sendMessageEvent(kafkaEvent);

            System.out.println("File message echoed immediately and sent to Kafka: " + echoResponse.getMessageId());

        } catch (Exception e) {
            System.err.println("Error handling file message: " + e.getMessage());
            e.printStackTrace();

            // Gửi error message về client
            try {
                UUID errorSenderId = extractUserIdFromPrincipalOrToken(principal, authHeader);
                
                Map<String, Object> errorResponse = Map.of(
                        "type", "ERROR",
                        "message", "Failed to send file message: " + e.getMessage(),
                        "timestamp", Instant.now()
                );

                messagingTemplate.convertAndSendToUser(
                        errorSenderId.toString(),
                        "/queue/errors",
                        errorResponse
                );
            } catch (Exception errorEx) {
                System.err.println("Failed to send error response: " + errorEx.getMessage());
            }
        }
    }

    // NEW: Handler cho format JSON mới với immediate echo
    @MessageMapping("/message.send")
    public void handleNewMessage(@Payload MessageEvent event, 
                                Principal principal,
                                @Header(value = "Authorization", required = false) String authHeader) {
        try {
            UUID senderId = extractUserIdFromPrincipalOrToken(principal, authHeader);
            System.out.println("Processing message from user: " + senderId);

            // 1. IMMEDIATE ECHO - Gửi phản hồi ngay lập tức cho người gửi (UX tốt)
            // Lấy thông tin user để tạo sender object
            UserDTO senderUser = userService.findById(senderId)
                    .map(user -> UserDTO.builder()
                            .user_id(user.getUser_id())
                            .username(user.getUsername())
                            .display_name(user.getDisplay_name())
                            .nickname(user.getNickname())
                            .avatar_url(user.getAvatar_url())
                            .created_at(user.getCreated_at() != null ? user.getCreated_at().toString() : null)
                            .build())
                    .orElse(null);

            MessageResponseDto echoResponse = MessageResponseDto.builder()
                    .messageId(Uuids.timeBased())
                    .conversationId(event.getPayload().getConversationId())
                    .content(event.getPayload().getContent())
                    .sender(senderUser)
                    .messageType("TEXT")
                    .createdAt(java.time.LocalDateTime.now())
                    .isDeleted(false)
                    .isForwarded(false)
                    .build();

            // Gửi echo về sender ngay lập tức
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/message-echo",
                    echoResponse
            );

            // 2. ASYNC PROCESSING - Chuyển đổi sang MessageRequest để xử lý qua Kafka
                MessageRequest messageRequest = MessageRequest.builder()
                    .conversationId(event.getPayload().getConversationId())
                    .content(event.getPayload().getContent())
                    .type(event.getPayload().getType()) // Changed from messageType to type
                    .mentionedUserIds(event.getPayload().getMentions())
                    .replyTo(event.getPayload().getReplyTo())
                    .senderId(senderId) // Đảm bảo senderId được set
                    .build();

            // 3. GỬI VÀO KAFKA - Để xử lý lưu trữ và broadcast
            MessageEvent kafkaEvent = MessageEvent.forKafkaProcessing(messageRequest);

            kafkaProducer.sendMessageEvent(kafkaEvent);
            System.out.println("Successfully sent to Kafka");

            System.out.println("Message echoed immediately and sent to Kafka for processing: " + echoResponse.getMessageId());

        } catch (Exception e) {
            System.err.println("Error handling new message: " + e.getMessage());
            e.printStackTrace();

            // Gửi error message về client - sử dụng helper method
            try {
                UUID errorSenderId = extractUserIdFromPrincipalOrToken(principal, authHeader);
                
                Map<String, Object> errorResponse = Map.of(
                        "type", "ERROR",
                        "message", "Failed to send message: " + e.getMessage(),
                        "timestamp", Instant.now()
                );

                messagingTemplate.convertAndSendToUser(
                        errorSenderId.toString(),
                        "/queue/errors",
                        errorResponse
                );
            } catch (Exception errorEx) {
                System.err.println("Failed to send error response: " + errorEx.getMessage());
            }
        }
    }

    @MessageMapping("/typing")
    public void handleTyping(@Payload TypingEvent event, 
                            Principal principal,
                            @Header(value = "Authorization", required = false) String authHeader) {
        try {
            UUID userId = extractUserIdFromPrincipalOrToken(principal, authHeader);
            event.setUserId(userId);

            // ONLY process typing: true events
            // typing: false will be handled automatically by Redis TTL expiration
            if (event.isTyping()) {
                // Get user information
                UserDTO userInfo = userService.findById(userId)
                        .map(user -> UserDTO.builder()
                                .user_id(user.getUser_id())
                                .username(user.getUsername())
                                .display_name(user.getDisplay_name())
                                .nickname(user.getNickname())
                                .avatar_url(user.getAvatar_url())
                                .created_at(user.getCreated_at() != null ? user.getCreated_at().toString() : null)
                                .build())
                        .orElse(null);

                // Set user info in event
                event.setUser(userInfo);
                
                // Update Redis state with 2s TTL
                typingIndicatorService.startTyping(event.getConversationId(), userId);
                System.out.println("User typing: " + userId + " (" + (userInfo != null ? userInfo.getDisplay_name() : "Unknown") + ")");
                
                // Broadcast typing: true to conversation members
                messagingTemplate.convertAndSend(
                        "/topic/conversation/" + event.getConversationId() + "/typing",
                        event
                );
                
                System.out.println("✅ Typing TRUE event processed for user: " + userId 
                    + " in conversation: " + event.getConversationId());
            } else {
                // Log but ignore typing: false events - Redis TTL will handle automatically
                System.out.println("⚠️ Ignored typing FALSE event for user: " + userId 
                    + " - Redis TTL will handle auto-expiry");
            }
            
        } catch (Exception e) {
            System.err.println("Error handling typing event: " + e.getMessage());
            e.printStackTrace();
        }
    }



    @MessageMapping("/request-online-status")
    public void handleRequestOnlineStatus(@Payload OnlineStatusRequest request, 
                                         Principal principal,
                                         @Header(value = "Authorization", required = false) String authHeader) {
        try {
            UUID userId = extractUserIdFromPrincipalOrToken(principal, authHeader);
            System.out.println("request-online-status"+ userId);
            // Use PresenceService to get online status
            Map<UUID, Boolean> statusMap = request.getUserIds().stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            presenceService::isUserOnline
                    ));

            // Gửi phản hồi qua WebSocket
            OnlineStatusResponse response = OnlineStatusResponse.builder()
                    .statusMap(statusMap)
                    .timestamp(Instant.now())
                    .build();
            System.out.println("OnlineStatusResponse:" + response);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/online-status",
                    response
            );
        } catch (Exception e) {
            System.err.println("Error handling request online status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/notification.read")
    public void handleNotificationRead(@Payload Map<String, Object> request, 
                                      Principal principal,
                                      @Header(value = "Authorization", required = false) String authHeader) {
        try {
            UUID userId = extractUserIdFromPrincipalOrToken(principal, authHeader);
            UUID notificationId = UUID.fromString((String) request.get("notificationId"));

            notificationService.markAsRead(userId, notificationId);
        } catch (Exception e) {
            System.err.println("Error handling notification read: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/notifications.read-all")
    public void handleMarkAllNotificationsRead(Principal principal,
                                              @Header(value = "Authorization", required = false) String authHeader) {
        try {
            UUID userId = extractUserIdFromPrincipalOrToken(principal, authHeader);
            notificationService.markAllAsRead(userId);
        } catch (Exception e) {
            System.err.println("Error handling mark all notifications read: " + e.getMessage());
            e.printStackTrace();
        }
    }    
    /**
     * BACKUP: Extract user ID from JWT token when Principal fails
     */
    private UUID extractUserIdFromJwtToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            if (jwtService.validateToken(token)) {
                UUID userId = jwtService.getUserIdFromToken(token);
                System.out.println("Successfully extracted userId from JWT backup: " + userId);
                return userId;
            }
        } catch (Exception e) {
            System.err.println("Failed to extract userId from JWT token: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Extract user ID with multiple fallback strategies
     */
    private UUID extractUserIdFromPrincipalOrToken(Principal principal, String authHeader) {
        System.out.println("extractUserIdFromPrincipalOrToken called");
        System.out.println("Principal: " + principal);
        System.out.println("Auth header present: " + (authHeader != null));
        
        // Strategy 1: Try Principal first
        if (principal != null) {
            try {
                if (principal instanceof com.chatapp.chat_service.security.WebSocketAuthInterceptor.UserPrincipal) {
                    com.chatapp.chat_service.security.WebSocketAuthInterceptor.UserPrincipal userPrincipal =
                            (com.chatapp.chat_service.security.WebSocketAuthInterceptor.UserPrincipal) principal;
                    System.out.println("Found UserPrincipal: " + userPrincipal.getUserId());
                    return userPrincipal.getUserId();
                }
                
                if (principal instanceof Authentication) {
                    Authentication auth = (Authentication) principal;
                    Object principalObj = auth.getPrincipal();
                    
                    if (principalObj instanceof com.chatapp.chat_service.security.WebSocketAuthInterceptor.UserPrincipal) {
                        UUID userId = ((com.chatapp.chat_service.security.WebSocketAuthInterceptor.UserPrincipal) principalObj).getUserId();
                        System.out.println("Extracted userId from Authentication: " + userId);
                        return userId;
                    }
                }
                
                String principalName = principal.getName();
                if (principalName != null && !principalName.trim().isEmpty()) {
                    try {
                        UUID userId = UUID.fromString(principalName);
                        System.out.println("Parsed UUID from principal name: " + userId);
                        return userId;
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid UUID format for principal name: " + principalName);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error extracting from Principal: " + e.getMessage());
            }
        }
        
        // Strategy 2: Try JWT token backup
        UUID userIdFromToken = extractUserIdFromJwtToken(authHeader);
        if (userIdFromToken != null) {
            return userIdFromToken;
        }
        
        // Strategy 3: Complete failure
        System.err.println("All authentication strategies failed");
        throw new IllegalStateException("Authentication required - no valid user identification found");
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(Principal principal,
                               @Header(value = "Authorization", required = false) String authHeader,
                               @Header(value = "simpSessionId", required = false) String sessionId,
                               @Payload(required = false) Map<String, Object> heartbeatData) {
        try {
            UUID userId = extractUserIdFromPrincipalOrToken(principal, authHeader);
            
            // Extract device info from heartbeat data
            String deviceInfo = null;
            if (heartbeatData != null && heartbeatData.containsKey("deviceInfo")) {
                deviceInfo = heartbeatData.get("deviceInfo").toString();
            }
            
            // Refresh user session to extend TTL
            if (sessionId != null) {
                // Refresh both presence and websocket sessions
                presenceService.refreshUserSession(userId, sessionId);
                webSocketConnectionService.refreshSession(userId, sessionId);
                
                // Update device info if provided
                if (deviceInfo != null) {
                    webSocketConnectionService.updateDeviceInfo(userId, sessionId, deviceInfo);
                }
                
                log.debug("Refreshed session for user {} with session {} and device {}", 
                         userId, sessionId, deviceInfo);
            } else {
                log.warn("Heartbeat received without session ID for user {}", userId);
            }
        } catch (Exception e) {
            System.err.println("Error handling heartbeat: " + e.getMessage());
            e.printStackTrace();
        }
    }
//
//    @MessageMapping("/presence.subscribe")
//    public void handlePresenceSubscribe(@Payload Map<String, Object> request,
//                                       Principal principal,
//                                       @Header(value = "Authorization", required = false) String authHeader) {
//        try {
//            UUID subscriberId = extractUserIdFromPrincipalOrToken(principal, authHeader);
//
//            @SuppressWarnings("unchecked")
//            List<String> userIds = (List<String>) request.get("userIds");
//
//            if (userIds != null) {
//                // Convert to UUID list for optimized batch subscription
//                java.util.List<UUID> targetUserIds = new java.util.ArrayList<>();
//                for (String userIdStr : userIds) {
//                    try {
//                        UUID watchedUserId = UUID.fromString(userIdStr);
//                        targetUserIds.add(watchedUserId);
//
//                        // Keep compatibility with old subscription service
//                        presenceSubscriptionService.subscribeToPresence(subscriberId, watchedUserId);
//
//                        log.debug("User {} subscribed to presence of user {}", subscriberId, watchedUserId);
//                    } catch (IllegalArgumentException e) {
//                        log.warn("Invalid UUID in presence subscribe: {}", userIdStr);
//                    }
//                }
//
//                // Use optimized batch subscription
//                if (!targetUserIds.isEmpty()) {
//                    presenceService.subscribeToUsers(subscriberId, targetUserIds);
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error handling presence subscribe: {}", e.getMessage(), e);
//        }
//    }
//
//    @MessageMapping("/presence.unsubscribe")
//    public void handlePresenceUnsubscribe(@Payload Map<String, Object> request,
//                                         Principal principal,
//                                         @Header(value = "Authorization", required = false) String authHeader) {
//        try {
//            UUID subscriberId = extractUserIdFromPrincipalOrToken(principal, authHeader);
//
//            @SuppressWarnings("unchecked")
//            List<String> userIds = (List<String>) request.get("userIds");
//
//            if (userIds != null) {
//                // Convert to UUID list for optimized batch unsubscription
//                java.util.List<UUID> targetUserIds = new java.util.ArrayList<>();
//                for (String userIdStr : userIds) {
//                    try {
//                        UUID watchedUserId = UUID.fromString(userIdStr);
//                        targetUserIds.add(watchedUserId);
//
//                        // Keep compatibility with old subscription service
//                        presenceSubscriptionService.unsubscribeFromPresence(subscriberId, watchedUserId);
//
//                        log.debug("User {} unsubscribed from presence of user {}", subscriberId, watchedUserId);
//                    } catch (IllegalArgumentException e) {
//                        log.warn("Invalid UUID in presence unsubscribe: {}", userIdStr);
//                    }
//                }
//
//                // Use optimized batch unsubscription
//                if (!targetUserIds.isEmpty()) {
//                    presenceService.unsubscribeFromUsers(subscriberId, targetUserIds);
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error handling presence unsubscribe: {}", e.getMessage(), e);
//        }
//    }
}