package com.chatapp.chat_service.redis.listener;

import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.auth.service.UserService;
import com.chatapp.chat_service.kafka.KafkaEventProducer;
import com.chatapp.chat_service.presence.event.OnlineStatusEvent;
import com.chatapp.chat_service.presence.service.PresenceService;
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
    private final PresenceService presenceService;
    private final KafkaEventProducer kafkaEventProducer;
    // Pattern to match typing keys: conversation:typing:conversationId:userId
    private static final Pattern TYPING_KEY_PATTERN = Pattern
            .compile("conversation:typing:([a-f0-9-]{36}):([a-f0-9-]{36})");
    private static final Pattern SESSION_KEY_PATTERN = Pattern.compile("presence:session:([a-f0-9-]{36}):(.+)");
    private static final String USER_ONLINE_PREFIX = "presence:user:";
    private static final String ONLINE_SUFFIX = ":online:";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.debug("Redis key expired: {}", expiredKey);

        Matcher typingMatcher = TYPING_KEY_PATTERN.matcher(expiredKey);
        if (typingMatcher.matches()) {
            String conversationId = typingMatcher.group(1);
            UUID userId = UUID.fromString(typingMatcher.group(2));
            handleTypingKeyExpired(conversationId, userId);
            return;
        }

        Matcher sessionMatcher = SESSION_KEY_PATTERN.matcher(expiredKey);
        if (sessionMatcher.matches()) {
            UUID userId = UUID.fromString(sessionMatcher.group(1));
            String sessionId = sessionMatcher.group(2);
            // Gọi service đã được thiết kế để xử lý việc này
            presenceService.handleExpiredSession(userId, sessionId);
        }
    }

    private void handleTypingKeyExpired(String conversationId, UUID userId) {
        try {
            UUID conversationUuid = UUID.fromString(conversationId);

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
            System.out.println(
                    "User: " + userId + " (" + (userInfo != null ? userInfo.getDisplay_name() : "Unknown") + ")");
            System.out.println("Broadcasting typing: false to all participants");

            // Create typing event with isTyping: false
            TypingEvent typingEvent = TypingEvent.builder()
                    .conversationId(conversationUuid)
                    .userId(userId)
                    .user(userInfo)
                    .typing(false)
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversationId + "/typing",
                    typingEvent);

            System.out.println("Successfully broadcasted auto typing: false for user: " + userId + " in conversation: "
                    + conversationId);

        } catch (Exception e) {
            System.err.println(
                    "Error handling typing key expiration for conversation: " + conversationId + ", user: " + userId);
            e.printStackTrace();
        }
    }

    private void handleUserSessionExpired(UUID userId, String sessionId) {
        try {
            log.info("=== SESSION EXPIRED - userId: {}, sessionId: {} ===", userId, sessionId);

            String userKey = userId.toString();

            String debounceKey = "debounce:offline:" + userKey;
            String lockKey = "lock:offline:" + userKey;

            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey, "processing", java.time.Duration.ofSeconds(15));

            if (Boolean.FALSE.equals(lockAcquired)) {
                log.info("Another instance is processing offline for user {}, skipping", userId);
                return;
            }

            try {
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

                redisTemplate.opsForValue().set(debounceKey,
                        String.valueOf(System.currentTimeMillis()),
                        java.time.Duration.ofSeconds(30));

                String userOnlinePattern = USER_ONLINE_PREFIX + userId + ONLINE_SUFFIX + "*";
                Set<String> activeSessions = redisTemplate.keys(userOnlinePattern);

                log.info("Active sessions check for user {}: pattern={}, found={}",
                        userId, userOnlinePattern, activeSessions != null ? activeSessions.size() : 0);

                if (activeSessions != null && !activeSessions.isEmpty()) {
                    log.info("User {} still has {} active sessions, NOT setting offline",
                            userId, activeSessions.size());
                    return;
                }

                Boolean isInOnlineSet = redisTemplate.opsForSet().isMember("presence:online", userKey);

                if (Boolean.FALSE.equals(isInOnlineSet)) {
                    log.info("User {} already offline in Redis, skipping event", userId);
                    return;
                }

                Long removedCount = redisTemplate.opsForSet().remove("presence:online", userKey);
                log.info("Removed user {} from online set, count: {}", userId, removedCount);

                if (removedCount == null || removedCount == 0) {
                    log.warn("User {} was not in online set, skipping offline event", userId);
                    return;
                }

                redisTemplate.opsForValue().set(
                        "presence:user:" + userId + ":last_active",
                        String.valueOf(Instant.now().toEpochMilli()));

                // Create and send offline event
                OnlineStatusEvent offlineEvent = OnlineStatusEvent.builder()
                        .userId(userId)
                        .online(false)
                        .timestamp(Instant.now())
                        .build();

                kafkaEventProducer.sendOnlineStatusEvent(offlineEvent);
                log.info("=== SENT OFFLINE EVENT === User: {}, Timestamp: {}",
                        userId, offlineEvent.getTimestamp());

            } finally {
                redisTemplate.delete(lockKey);
            }
        } catch (Exception e) {
            log.error("ERROR handling user session expiration for userId {}: {}", userId, e.getMessage(), e);
        }
    }
}
