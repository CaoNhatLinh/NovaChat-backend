package com.chatapp.chat_service.redis;

import com.chatapp.chat_service.kafka.messaging.KafkaMessageProducer;
import com.chatapp.chat_service.model.dto.UserDTO;
import com.chatapp.chat_service.service.UserService;
import com.chatapp.chat_service.websocket.event.OnlineStatusEvent;
import com.chatapp.chat_service.websocket.event.TypingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisKeyExpirationListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaMessageProducer kafkaProducer;

    // Pattern to match typing keys: conversation:typing:conversationId:userId
    private static final Pattern TYPING_KEY_PATTERN = Pattern.compile("conversation:typing:([a-f0-9-]{36}):([a-f0-9-]{36})");

    // Pattern to match presence session keys: presence:user:{userId}:online:{sessionId}
    private static final Pattern PRESENCE_SESSION_PATTERN = Pattern.compile("presence:user:([a-f0-9-]{36}):online:(.+)");
    
    private static final String USER_ONLINE_PREFIX = "presence:user:";
    private static final String ONLINE_SUFFIX = ":online:";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.debug("Redis key expired: {}", expiredKey);

        // Check if this is a typing key
        Matcher typingMatcher = TYPING_KEY_PATTERN.matcher(expiredKey);
        if (typingMatcher.matches()) {
            String conversationId = typingMatcher.group(1);
            UUID userId = UUID.fromString(typingMatcher.group(2));
            handleTypingKeyExpired(conversationId, userId);
            return;
        }
        
        // Check if this is a user presence session key
        Matcher presenceMatcher = PRESENCE_SESSION_PATTERN.matcher(expiredKey);
        if (presenceMatcher.matches()) {
            UUID userId = UUID.fromString(presenceMatcher.group(1));
            String sessionId = presenceMatcher.group(2);
            handleUserSessionExpired(userId, sessionId);
        }
    }
    
    private void handleTypingKeyExpired(String conversationId, UUID userId) {
        try {
            UUID conversationUuid = UUID.fromString(conversationId);
            
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
            
            System.out.println("Typing timeout for conversation: " + conversationId);
            System.out.println("User: " + userId + " (" + (userInfo != null ? userInfo.getDisplay_name() : "Unknown") + ")");
            System.out.println("Broadcasting typing: false to all participants");
            
            // Create typing event with isTyping: false
            TypingEvent typingEvent = TypingEvent.builder()
                    .conversationId(conversationUuid)
                    .userId(userId)
                    .user(userInfo)
                    .typing(false)
                    .build();
            
            // Broadcast to all conversation participants
            messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/typing",
                typingEvent
            );
            
            System.out.println("Successfully broadcasted auto typing: false for user: " + userId + " in conversation: " + conversationId);
            
        } catch (Exception e) {
            System.err.println("Error handling typing key expiration for conversation: " + conversationId + ", user: " + userId);
            e.printStackTrace();
        }
    }
    
    private void handleUserSessionExpired(UUID userId, String sessionId) {
        try {
            log.info("=== SESSION EXPIRED - userId: {}, sessionId: {} ===", userId, sessionId);
            
            String userKey = userId.toString();
            
            // CRITICAL: Strong debounce with unique key per user + timestamp window
            String debounceKey = "debounce:offline:" + userKey;
            String lockKey = "lock:offline:" + userKey;
            
            // Attempt to acquire exclusive lock for 15 seconds
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey, "processing", java.time.Duration.ofSeconds(15));
            
            if (Boolean.FALSE.equals(lockAcquired)) {
                log.info("Another instance is processing offline for user {}, skipping", userId);
                return;
            }
            
            try {
                // Double-check if we've processed this recently
                String lastProcessed = redisTemplate.opsForValue().get(debounceKey);
                if (lastProcessed != null) {
                    long lastTime = Long.parseLong(lastProcessed);
                    long timeDiff = System.currentTimeMillis() - lastTime;
                    if (timeDiff < 15000) { // 15 seconds
                        log.info("User {} offline event processed {} ms ago, skipping duplicate", 
                                userId, timeDiff);
                        return;
                    }
                }
                
                // Mark as processing
                redisTemplate.opsForValue().set(debounceKey, 
                    String.valueOf(System.currentTimeMillis()), 
                    java.time.Duration.ofSeconds(30));
                
                // Check if user has any other active sessions
                String userOnlinePattern = USER_ONLINE_PREFIX + userId + ONLINE_SUFFIX + "*";
                Set<String> activeSessions = redisTemplate.keys(userOnlinePattern);
                
                log.info("Active sessions check for user {}: pattern={}, found={}", 
                    userId, userOnlinePattern, activeSessions != null ? activeSessions.size() : 0);
                
                if (activeSessions != null && !activeSessions.isEmpty()) {
                    log.info("User {} still has {} active sessions, NOT setting offline", 
                            userId, activeSessions.size());
                    return;
                }
                
                // Final check: is user actually in online set?
                Boolean isInOnlineSet = redisTemplate.opsForSet().isMember("presence:online", userKey);
                
                if (Boolean.FALSE.equals(isInOnlineSet)) {
                    log.info("User {} already offline in Redis, skipping event", userId);
                    return;
                }
                
                // Remove from global online set
                Long removedCount = redisTemplate.opsForSet().remove("presence:online", userKey);
                log.info("Removed user {} from online set, count: {}", userId, removedCount);
                
                if (removedCount == null || removedCount == 0) {
                    log.warn("User {} was not in online set, skipping offline event", userId);
                    return;
                }
                
                // Update last active timestamp
                redisTemplate.opsForValue().set(
                    "presence:user:" + userId + ":last_active", 
                    String.valueOf(Instant.now().toEpochMilli())
                );
                
                // Create and send offline event
                OnlineStatusEvent offlineEvent = OnlineStatusEvent.builder()
                    .userId(userId)
                    .online(false)
                    .timestamp(Instant.now())
                    .build();
                
                kafkaProducer.sendOnlineStatusEvent(offlineEvent);
                log.info("=== SENT OFFLINE EVENT === User: {}, Timestamp: {}", 
                        userId, offlineEvent.getTimestamp());
                
            } finally {
                // Always release the lock
                redisTemplate.delete(lockKey);
            }
            
        } catch (Exception e) {
            log.error("ERROR handling user session expiration for userId {}: {}", userId, e.getMessage(), e);
        }
    }
}
