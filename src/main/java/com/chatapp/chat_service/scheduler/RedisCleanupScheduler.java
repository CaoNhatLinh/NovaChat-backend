package com.chatapp.chat_service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCleanupScheduler {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Clean up expired debounce keys every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredDebounceKeys() {
        try {
            // Clean up old debounce keys
            Set<String> debounceKeys = redisTemplate.keys("debounce:offline:*");
            if (debounceKeys != null && !debounceKeys.isEmpty()) {
                long currentTime = System.currentTimeMillis();
                int cleaned = 0;
                
                for (String key : debounceKeys) {
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        try {
                            long timestamp = Long.parseLong(value);
                            // Remove keys older than 1 hour
                            if (currentTime - timestamp > 3600000) {
                                redisTemplate.delete(key);
                                cleaned++;
                            }
                        } catch (NumberFormatException e) {
                            // Invalid timestamp, remove the key
                            redisTemplate.delete(key);
                            cleaned++;
                        }
                    }
                }
                
                if (cleaned > 0) {
                    log.info("Cleaned up {} expired debounce keys", cleaned);
                }
            }
            
            // Clean up old lock keys
            Set<String> lockKeys = redisTemplate.keys("lock:offline:*");
            if (lockKeys != null && !lockKeys.isEmpty()) {
                int cleaned = redisTemplate.delete(lockKeys).intValue();
                if (cleaned > 0) {
                    log.info("Cleaned up {} orphaned lock keys", cleaned);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during Redis cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Validate and clean up presence data every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void validatePresenceData() {
        try {
            Set<String> onlineUsers = redisTemplate.opsForSet().members("presence:online");
            if (onlineUsers != null) {
                int validUsers = 0;
                int removedUsers = 0;
                
                for (String userId : onlineUsers) {
                    // Check if user has any active sessions
                    Set<String> activeSessions = redisTemplate.keys("presence:user:" + userId + ":online:*");
                    
                    if (activeSessions == null || activeSessions.isEmpty()) {
                        // No active sessions, remove from online set
                        redisTemplate.opsForSet().remove("presence:online", userId);
                        removedUsers++;
                        log.debug("Removed user {} from online set - no active sessions", userId);
                    } else {
                        validUsers++;
                    }
                }
                
                log.info("Presence validation: {} valid users, {} removed users", validUsers, removedUsers);
            }
            
        } catch (Exception e) {
            log.error("Error during presence validation: {}", e.getMessage(), e);
        }
    }
}
