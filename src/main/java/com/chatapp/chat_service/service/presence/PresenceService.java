package com.chatapp.chat_service.service.presence;

import com.chatapp.chat_service.kafka.messaging.KafkaMessageProducer;
import com.chatapp.chat_service.model.dto.UserPresenceResponse;
import com.chatapp.chat_service.repository.FriendshipRepository;
import com.chatapp.chat_service.repository.ConversationMemberRepository;
import com.chatapp.chat_service.websocket.event.OnlineStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎯 Presence Service - Tối ưu cho production
 * 
 * Core Functions:
 * 1. Session-based multi-device presence tracking (60s TTL)
 * 2. Subscription management for real-time updates
 * 3. Batch presence queries for friends/conversations
 * 4. Automatic offline detection via Redis expiration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final FriendshipRepository friendshipRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final KafkaMessageProducer kafkaProducer;
    
    // 🔑 Optimized Redis Key Patterns
    private static final String USER_SESSIONS_KEY = "presence:user:%s:sessions";     // Set of active sessions
    private static final String SESSION_KEY = "presence:session:%s";                // Individual session data
    private static final String SUBSCRIPTIONS_KEY = "presence:subs:%s";             // User's subscriptions
    private static final String SUBSCRIBERS_KEY = "presence:watchers:%s";           // User's watchers
    
    // ⏰ TTL Constants
    private static final long SESSION_TTL_SECONDS = 60L;        // Session expires in 60s
    private static final long SUBSCRIPTION_TTL_HOURS = 24L;     // Subscriptions last 24h
    
    // ===============================
    // 🟢 CORE PRESENCE MANAGEMENT
    // ===============================
    
    /**
     * 🟢 User comes online - Multi-device support
     */
    public void setUserOnline(UUID userId, String sessionId, String deviceInfo) {
        try {
            String userSessionsKey = String.format(USER_SESSIONS_KEY, userId);
            String sessionKey = String.format(SESSION_KEY, sessionId);
            
            // 1. Add session to user's active sessions
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, Duration.ofSeconds(SESSION_TTL_SECONDS + 10)); // Buffer
            
            // 2. Store session details with TTL
            Map<String, String> sessionData = Map.of(
                "userId", userId.toString(),
                "deviceInfo", deviceInfo != null ? deviceInfo : "unknown",
                "lastActive", Instant.now().toString()
            );
            redisTemplate.opsForHash().putAll(sessionKey, sessionData);
            redisTemplate.expire(sessionKey, Duration.ofSeconds(SESSION_TTL_SECONDS));
            
            // 3. Send online event via Kafka (only if first session)
            Set<String> sessions = redisTemplate.opsForSet().members(userSessionsKey);
            if (sessions != null && sessions.size() == 1) { // First session
                sendOnlineStatusEvent(userId, true);
            }
            
            log.debug("User {} online with session {} (total sessions: {})", 
                     userId, sessionId, sessions != null ? sessions.size() : 1);
            
        } catch (Exception e) {
            log.error("Error setting user {} online", userId, e);
        }
    }
    
    /**
     * 🔄 Refresh user session - Called by heartbeat
     */
    public void refreshUserSession(UUID userId, String sessionId) {
        try {
            String sessionKey = String.format(SESSION_KEY, sessionId);
            String userSessionsKey = String.format(USER_SESSIONS_KEY, userId);
            
            // Check if session exists
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                // Refresh session TTL
                redisTemplate.expire(sessionKey, Duration.ofSeconds(SESSION_TTL_SECONDS));
                redisTemplate.expire(userSessionsKey, Duration.ofSeconds(SESSION_TTL_SECONDS + 10));
                
                // Update last active
                redisTemplate.opsForHash().put(sessionKey, "lastActive", Instant.now().toString());
                
                log.debug("Refreshed session {} for user {}", sessionId, userId);
            } else {
                log.warn("Session {} not found for user {} - might be expired", sessionId, userId);
            }
            
        } catch (Exception e) {
            log.error("Error refreshing session {} for user {}", sessionId, userId, e);
        }
    }
    
    /**
     * 🔴 User goes offline - Handle session cleanup
     */
    public void setUserOffline(UUID userId, String sessionId) {
        try {
            String userSessionsKey = String.format(USER_SESSIONS_KEY, userId);
            String sessionKey = String.format(SESSION_KEY, sessionId);
            
            // 1. Remove session from user's active sessions
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            redisTemplate.delete(sessionKey);
            
            // 2. Check if user has other active sessions
            Set<String> remainingSessions = redisTemplate.opsForSet().members(userSessionsKey);
            if (remainingSessions == null || remainingSessions.isEmpty()) {
                // No more sessions - user is offline
                redisTemplate.delete(userSessionsKey);
                sendOnlineStatusEvent(userId, false);
                log.debug("User {} went offline (no more sessions)", userId);
            } else {
                log.debug("User {} still online with {} sessions", userId, remainingSessions.size());
            }
            
        } catch (Exception e) {
            log.error("Error setting user {} offline", userId, e);
        }
    }
    
    /**
     * ✅ Check if user is online
     */
    public boolean isUserOnline(UUID userId) {
        try {
            String userSessionsKey = String.format(USER_SESSIONS_KEY, userId);
            Set<String> sessions = redisTemplate.opsForSet().members(userSessionsKey);
            return sessions != null && !sessions.isEmpty();
        } catch (Exception e) {
            log.error("Error checking online status for user {}", userId, e);
            return false;
        }
    }
    
    /**
     * 📱 Get user's active sessions
     */
    public Set<String> getUserActiveSessions(UUID userId) {
        try {
            String userSessionsKey = String.format(USER_SESSIONS_KEY, userId);
            return redisTemplate.opsForSet().members(userSessionsKey);
        } catch (Exception e) {
            log.error("Error getting sessions for user {}", userId, e);
            return Collections.emptySet();
        }
    }
    
    // ===============================
    // 📡 SUBSCRIPTION MANAGEMENT
    // ===============================
    
    /**
     * 📡 Subscribe to users' presence updates
     */
    public void subscribeToUsers(UUID subscriberId, List<UUID> targetUserIds) {
        try {
            // Remove duplicates
            Set<UUID> uniqueTargets = new HashSet<>(targetUserIds);
            
            String subscriptionsKey = String.format(SUBSCRIPTIONS_KEY, subscriberId);
            
            for (UUID targetUserId : uniqueTargets) {
                // Add to subscriber's subscription list
                redisTemplate.opsForSet().add(subscriptionsKey, targetUserId.toString());
                
                // Add subscriber to target's watchers list
                String watchersKey = String.format(SUBSCRIBERS_KEY, targetUserId);
                redisTemplate.opsForSet().add(watchersKey, subscriberId.toString());
                redisTemplate.expire(watchersKey, Duration.ofHours(SUBSCRIPTION_TTL_HOURS));
            }
            
            redisTemplate.expire(subscriptionsKey, Duration.ofHours(SUBSCRIPTION_TTL_HOURS));
            
            log.debug("User {} subscribed to {} users", subscriberId, uniqueTargets.size());
            
        } catch (Exception e) {
            log.error("Error subscribing user {} to presence updates", subscriberId, e);
        }
    }
    
    /**
     * 🚫 Unsubscribe from users' presence updates
     */
    public void unsubscribeFromUsers(UUID subscriberId, List<UUID> targetUserIds) {
        try {
            String subscriptionsKey = String.format(SUBSCRIPTIONS_KEY, subscriberId);
            
            for (UUID targetUserId : targetUserIds) {
                // Remove from subscriber's subscription list
                redisTemplate.opsForSet().remove(subscriptionsKey, targetUserId.toString());
                
                // Remove subscriber from target's watchers list
                String watchersKey = String.format(SUBSCRIBERS_KEY, targetUserId);
                redisTemplate.opsForSet().remove(watchersKey, subscriberId.toString());
            }
            
            log.debug("User {} unsubscribed from {} users", subscriberId, targetUserIds.size());
            
        } catch (Exception e) {
            log.error("Error unsubscribing user {} from presence updates", subscriberId, e);
        }
    }
    
    /**
     * 📋 Get users watching this user (for sending updates)
     */
    public Set<UUID> getWatchers(UUID userId) {
        try {
            String watchersKey = String.format(SUBSCRIBERS_KEY, userId);
            Set<String> watcherIds = redisTemplate.opsForSet().members(watchersKey);
            
            if (watcherIds == null || watcherIds.isEmpty()) {
                return Collections.emptySet();
            }
            
            return watcherIds.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
                    
        } catch (Exception e) {
            log.error("Error getting watchers for user {}", userId, e);
            return Collections.emptySet();
        }
    }
    
    // ===============================
    // 📊 BATCH PRESENCE QUERIES
    // ===============================
    
    /**
     * 📊 Get presence for multiple users (optimized batch query)
     */
    public Map<UUID, UserPresenceResponse> getBatchPresence(List<UUID> userIds) {
        Map<UUID, UserPresenceResponse> result = new HashMap<>();
        
        try {
            for (UUID userId : userIds) {
                boolean isOnline = isUserOnline(userId);
                
                UserPresenceResponse presence = UserPresenceResponse.builder()
                        .userId(userId)
                        .status(isOnline ? "ONLINE" : "OFFLINE")
                        .isOnline(isOnline)
                        .lastSeen(isOnline ? null : Instant.now())
                        .lastActiveAgo(isOnline ? null : "Unknown")
                        .build();
                
                result.put(userId, presence);
            }
            
        } catch (Exception e) {
            log.error("Error getting batch presence", e);
        }
        
        return result;
    }
    
    /**
     * 👥 Get friends' presence
     */
    public Map<UUID, UserPresenceResponse> getFriendsPresence(UUID userId) {
        try {
            // Get friend IDs from database
            List<UUID> friendIds = friendshipRepository.findAcceptedFriendIds(userId);
            return getBatchPresence(friendIds);
            
        } catch (Exception e) {
            log.error("Error getting friends presence for user {}", userId, e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * 💬 Get conversation members' presence
     */
    public Map<UUID, UserPresenceResponse> getConversationMembersPresence(UUID conversationId, UUID requesterId) {
        try {
            // Get member IDs from database
            List<UUID> memberIds = conversationMemberRepository.findByKeyConversationId(conversationId)
                    .stream()
                    .map(member -> member.getKey().getUserId())
                    .collect(Collectors.toList());
            return getBatchPresence(memberIds);
            
        } catch (Exception e) {
            log.error("Error getting conversation {} members presence", conversationId, e);
            return Collections.emptyMap();
        }
    }
    
    // ===============================
    // � WATCHER MANAGEMENT (Compatibility)
    // ===============================
    
    /**
     * Add watcher for user presence (compatibility method)
     */
    // public void addPresenceWatcher(UUID watchedUserId, UUID watcherUserId) {
    //     try {
    //         // Add to watchers set
    //         String watchersKey = String.format(SUBSCRIBERS_KEY, watchedUserId);
    //         redisTemplate.opsForSet().add(watchersKey, watcherUserId.toString());
    //         redisTemplate.expire(watchersKey, Duration.ofHours(SUBSCRIPTION_TTL_HOURS));
            
    //         log.debug("Added watcher {} for user {}", watcherUserId, watchedUserId);
            
    //     } catch (Exception e) {
    //         log.error("Error adding watcher {} for user {}", watcherUserId, watchedUserId, e);
    //     }
    // }
    
    // /**
    //  * Remove watcher for user presence (compatibility method)
    //  */
    // public void removePresenceWatcher(UUID watchedUserId, UUID watcherUserId) {
    //     try {
    //         // Remove from watchers set
    //         String watchersKey = String.format(SUBSCRIBERS_KEY, watchedUserId);
    //         redisTemplate.opsForSet().remove(watchersKey, watcherUserId.toString());
            
    //         log.debug("Removed watcher {} from user {}", watcherUserId, watchedUserId);
            
    //     } catch (Exception e) {
    //         log.error("Error removing watcher {} from user {}", watcherUserId, watchedUserId, e);
    //     }
    // }
    
    // ===============================
    // �🚀 KAFKA EVENT HANDLING
    // ===============================
    
    /**
     * 📢 Send online status event via Kafka
     */
    private void sendOnlineStatusEvent(UUID userId, boolean isOnline) {
        try {
            OnlineStatusEvent event = OnlineStatusEvent.builder()
                    .userId(userId)
                    .online(isOnline)
                    .timestamp(Instant.now())
                    .build();
            
            kafkaProducer.sendOnlineStatusEvent(event);
            log.debug("Sent online status event: user={}, online={}", userId, isOnline);
            
        } catch (Exception e) {
            log.error("Error sending online status event for user {}", userId, e);
        }
    }
}
