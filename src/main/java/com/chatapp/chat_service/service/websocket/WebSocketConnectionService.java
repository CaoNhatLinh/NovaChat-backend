package com.chatapp.chat_service.service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quản lý các kết nối WebSocket của users với session-based tracking
 * Hỗ trợ đa thiết bị với TTL 60s cho mỗi session
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectionService {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis key patterns
    private static final String WS_CONNECTIONS_KEY = "user:ws:connections";
    private static final String WS_SESSION_PREFIX = "ws:session:";
    private static final String WS_USER_SESSIONS_PREFIX = "ws:user:sessions:";

    // Session TTL in seconds (60 seconds)
    private static final long SESSION_TTL = 60L;

    /**
     * Đăng ký kết nối WebSocket mới cho user với session ID
     * @param userId ID của user
     * @param sessionId Session ID của kết nối
     * @param device Thông tin thiết bị
     */
    public void registerConnection(UUID userId, String sessionId, String device) {
        String sessionKey = WS_SESSION_PREFIX + sessionId;
        String userSessionsKey = WS_USER_SESSIONS_PREFIX + userId;

        // Lưu thông tin session với TTL
        String sessionData = String.format("userId=%s,device=%s,timestamp=%s",
                                         userId.toString(), device, Instant.now().toEpochMilli());
        redisTemplate.opsForValue().set(sessionKey, sessionData, Duration.ofSeconds(SESSION_TTL));

        // Thêm session vào danh sách sessions của user
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, Duration.ofSeconds(SESSION_TTL + 30)); // TTL dài hơn một chút

        // Cập nhật connection count (backward compatibility)
        String countKey = WS_CONNECTIONS_KEY + ":" + userId;
        Long newCount = redisTemplate.opsForValue().increment(countKey, 1);
        redisTemplate.expire(countKey, Duration.ofSeconds(SESSION_TTL + 30));

        log.info("Registered WebSocket connection for user: {}, session: {}, device: {}, total connections: {}",
                userId, sessionId, device, newCount);
    }

    /**
     * Overloaded method for backward compatibility
     */
    public void registerConnection(UUID userId) {
        String sessionId = UUID.randomUUID().toString();
        registerConnection(userId, sessionId, "unknown");
    }

    /**
     * Hủy đăng ký kết nối WebSocket của user với session ID
     * @param userId ID của user
     * @param sessionId Session ID của kết nối
     */
    public void unregisterConnection(UUID userId, String sessionId) {
        String sessionKey = WS_SESSION_PREFIX + sessionId;
        String userSessionsKey = WS_USER_SESSIONS_PREFIX + userId;

        // Xóa session data
        redisTemplate.delete(sessionKey);

        // Xóa session khỏi danh sách sessions của user
        redisTemplate.opsForSet().remove(userSessionsKey, sessionId);

        // Cập nhật connection count
        String countKey = WS_CONNECTIONS_KEY + ":" + userId;
        Long remainingCount = redisTemplate.opsForValue().decrement(countKey, 1);

        if (remainingCount != null && remainingCount <= 0) {
            redisTemplate.delete(countKey);
            redisTemplate.delete(userSessionsKey);
            log.info("Removed all WebSocket connections for user: {}", userId);
        } else {
            log.debug("Unregistered WebSocket connection for user: {}, session: {}, remaining connections: {}",
                     userId, sessionId, remainingCount);
        }
    }

    /**
     * Overloaded method for backward compatibility
     */
    public void unregisterConnection(UUID userId) {
        // Xóa tất cả sessions của user
        clearAllConnections(userId);
    }

    /**
     * Refresh session TTL (heartbeat support)
     * @param userId ID của user
     * @param sessionId Session ID cần refresh
     */
    public void refreshSession(UUID userId, String sessionId) {
        String sessionKey = WS_SESSION_PREFIX + sessionId;
        String userSessionsKey = WS_USER_SESSIONS_PREFIX + userId;

        // Kiểm tra session có tồn tại không
        if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
            // Extend TTL cho session
            redisTemplate.expire(sessionKey, Duration.ofSeconds(SESSION_TTL));
            redisTemplate.expire(userSessionsKey, Duration.ofSeconds(SESSION_TTL + 30));

            // Cập nhật timestamp trong session data
            String sessionData = redisTemplate.opsForValue().get(sessionKey);
            if (sessionData != null) {
                String[] parts = sessionData.split(",");
                if (parts.length >= 2) {
                    String newSessionData = String.format("userId=%s,device=%s,timestamp=%s",
                                                        userId.toString(),
                                                        parts[1].split("=")[1],
                                                        Instant.now().toEpochMilli());
                    redisTemplate.opsForValue().set(sessionKey, newSessionData, Duration.ofSeconds(SESSION_TTL));
                }
            }

            log.debug("Refreshed session for user: {}, session: {}", userId, sessionId);
        } else {
            log.warn("Attempted to refresh non-existent session: {} for user: {}", sessionId, userId);
        }
    }

    /**
     * Kiểm tra user có kết nối WebSocket active không
     * @param userId ID của user
     * @return true nếu user có ít nhất 1 kết nối active
     */
    public boolean hasActiveConnection(UUID userId) {
        String userSessionsKey = WS_USER_SESSIONS_PREFIX + userId;
        Set<String> sessions = redisTemplate.opsForSet().members(userSessionsKey);

        if (sessions == null || sessions.isEmpty()) {
            return false;
        }

        // Kiểm tra từng session có còn active không
        for (String sessionId : sessions) {
            String sessionKey = WS_SESSION_PREFIX + sessionId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                return true;
            }
        }

        // Nếu không có session nào active, cleanup
        redisTemplate.delete(userSessionsKey);
        String countKey = WS_CONNECTIONS_KEY + ":" + userId;
        redisTemplate.delete(countKey);

        return false;
    }

    /**
     * Lấy số lượng kết nối active của user
     * @param userId ID của user
     * @return số lượng kết nối active
     */
    public long getActiveConnectionCount(UUID userId) {
        String userSessionsKey = WS_USER_SESSIONS_PREFIX + userId;
        Set<String> sessions = redisTemplate.opsForSet().members(userSessionsKey);

        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }

        // Đếm các session còn active
        long activeCount = 0;
        List<String> expiredSessions = new ArrayList<>();

        for (String sessionId : sessions) {
            String sessionKey = WS_SESSION_PREFIX + sessionId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                activeCount++;
            } else {
                expiredSessions.add(sessionId);
            }
        }

        // Cleanup expired sessions
        if (!expiredSessions.isEmpty()) {
            redisTemplate.opsForSet().remove(userSessionsKey, expiredSessions.toArray());
        }

        return activeCount;
    }

    /**
     * Lấy danh sách active sessions của user
     * @param userId ID của user
     * @return Set các session IDs active
     */
    public Set<String> getActiveSessions(UUID userId) {
        String userSessionsKey = WS_USER_SESSIONS_PREFIX + userId;
        Set<String> sessions = redisTemplate.opsForSet().members(userSessionsKey);

        if (sessions == null || sessions.isEmpty()) {
            return new HashSet<>();
        }

        // Lọc các session còn active
        Set<String> activeSessions = new HashSet<>();
        List<String> expiredSessions = new ArrayList<>();

        for (String sessionId : sessions) {
            String sessionKey = WS_SESSION_PREFIX + sessionId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                activeSessions.add(sessionId);
            } else {
                expiredSessions.add(sessionId);
            }
        }

        // Cleanup expired sessions
        if (!expiredSessions.isEmpty()) {
            redisTemplate.opsForSet().remove(userSessionsKey, expiredSessions.toArray());
        }

        return activeSessions;
    }

    /**
     * Lấy thông tin chi tiết về session
     * @param sessionId Session ID
     * @return Map chứa thông tin session hoặc null nếu không tồn tại
     */
    public Map<String, String> getSessionInfo(String sessionId) {
        String sessionKey = WS_SESSION_PREFIX + sessionId;
        String sessionData = redisTemplate.opsForValue().get(sessionKey);

        if (sessionData == null) {
            return null;
        }

        Map<String, String> sessionInfo = new HashMap<>();
        String[] parts = sessionData.split(",");

        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                sessionInfo.put(keyValue[0], keyValue[1]);
            }
        }

        return sessionInfo;
    }

    /**
     * Xóa tất cả kết nối của user (cleanup)
     * @param userId ID của user
     */
    public void clearAllConnections(UUID userId) {
        String userSessionsKey = WS_USER_SESSIONS_PREFIX + userId;
        Set<String> sessions = redisTemplate.opsForSet().members(userSessionsKey);

        if (sessions != null && !sessions.isEmpty()) {
            // Xóa tất cả session data
            for (String sessionId : sessions) {
                String sessionKey = WS_SESSION_PREFIX + sessionId;
                redisTemplate.delete(sessionKey);
            }
        }

        // Xóa user sessions set và connection count
        redisTemplate.delete(userSessionsKey);
        String countKey = WS_CONNECTIONS_KEY + ":" + userId;
        redisTemplate.delete(countKey);

        log.info("Cleared all WebSocket connections for user: {}", userId);
    }

    /**
     * Cleanup expired sessions cho maintenance
     */
    public void cleanupExpiredSessions() {
        log.info("Starting cleanup of expired WebSocket sessions");

        // Tìm tất cả user sessions keys
        String pattern = WS_USER_SESSIONS_PREFIX + "*";
        Set<String> userSessionKeys = redisTemplate.keys(pattern);

        if (userSessionKeys == null || userSessionKeys.isEmpty()) {
            return;
        }

        int cleanedUsers = 0;
        int cleanedSessions = 0;

        for (String userSessionKey : userSessionKeys) {
            Set<String> sessions = redisTemplate.opsForSet().members(userSessionKey);

            if (sessions != null && !sessions.isEmpty()) {
                List<String> expiredSessions = new ArrayList<>();

                for (String sessionId : sessions) {
                    String sessionKey = WS_SESSION_PREFIX + sessionId;
                    if (!Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                        expiredSessions.add(sessionId);
                        cleanedSessions++;
                    }
                }

                // Cleanup expired sessions
                if (!expiredSessions.isEmpty()) {
                    redisTemplate.opsForSet().remove(userSessionKey, expiredSessions.toArray());
                }

                // Nếu không còn session nào active, xóa user sessions key
                if (sessions.size() == expiredSessions.size()) {
                    redisTemplate.delete(userSessionKey);

                    // Xóa connection count key
                    String userId = userSessionKey.replace(WS_USER_SESSIONS_PREFIX, "");
                    String countKey = WS_CONNECTIONS_KEY + ":" + userId;
                    redisTemplate.delete(countKey);

                    cleanedUsers++;
                }
            }
        }

        log.info("Cleanup completed: {} users, {} sessions cleaned", cleanedUsers, cleanedSessions);
    }

    /**
     * Cập nhật device info cho session
     */
    public void updateDeviceInfo(UUID userId, String sessionId, String deviceInfo) {
        try {
            String sessionKey = WS_SESSION_PREFIX + sessionId;

            // Check if session exists
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                String sessionData = redisTemplate.opsForValue().get(sessionKey);
                if (sessionData != null) {
                    // Update device info in session data
                    String[] parts = sessionData.split(",");
                    if (parts.length >= 2) {
                        String newSessionData = String.format("userId=%s,device=%s,timestamp=%s",
                                                            userId.toString(),
                                                            deviceInfo,
                                                            Instant.now().toEpochMilli());
                        redisTemplate.opsForValue().set(sessionKey, newSessionData, Duration.ofSeconds(SESSION_TTL));
                        log.debug("Updated device info for user: {}, session: {}, device: {}",
                                 userId, sessionId, deviceInfo);
                    }
                }
            } else {
                log.warn("Attempted to update device info for non-existent session: {} for user: {}",
                        sessionId, userId);
            }
        } catch (Exception e) {
            log.error("Error updating device info for user: {}, session: {}", userId, sessionId, e);
        }
    }
}
