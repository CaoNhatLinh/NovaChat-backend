package com.chatapp.chat_service.presence.service;

import com.chatapp.chat_service.kafka.KafkaEventProducer;
import com.chatapp.chat_service.presence.dto.UserPresenceResponse;
import com.chatapp.chat_service.presence.entity.UserPresence;
import com.chatapp.chat_service.presence.event.OnlineStatusEvent;
import com.chatapp.chat_service.presence.repository.UserPresenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaEventProducer kafkaEventProducer;
    private final UserPresenceRepository userPresenceRepository; // Để check privacy

    // ----------------------------------------------------------------
    // 🔑 CÁC KEY REDIS ĐƯỢC THIẾT KẾ LẠI
    // ----------------------------------------------------------------
    
    // Set<SessionID> - Lưu các session đang online của 1 user
    // Dùng để check N+1 Pipelining
    private static final String USER_SESSIONS_KEY = "presence:sessions:%s"; // (UUID: userId)
    
    // String - Key "nhịp tim" (heartbeat) cho 1 session cụ thể.
    // Key này sẽ TỰ HỦY (expire) sau 60s.
    private static final String SESSION_HEARTBEAT_KEY = "presence:hb:%s:%s"; // (UUID: userId, String: sessionId)
    
    // Set<UUID> - Lưu danh sách user ID mà TÔI (subscriber) đang theo dõi
    private static final String MY_SUBSCRIPTIONS_KEY = "presence:subs:%s"; // (UUID: subscriberId)
    
    // Set<UUID> - Lưu danh sách user ID đang theo dõi TÔI (watchers)
    private static final String MY_WATCHERS_KEY = "presence:watchers:%s"; // (UUID: targetUserId)

    // ----------------------------------------------------------------
    // 1. QUẢN LÝ ONLINE/OFFLINE (HEARTBEAT)
    // ----------------------------------------------------------------

    /**
     * Kiểm tra user có online không
     */
    public boolean isUserOnline(UUID userId) {
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);
        Long sessionCount = redisTemplate.opsForSet().size(sessionsKey);
        return sessionCount != null && sessionCount > 0;
    }
    public void setUserOnlineStatus(UUID userId, boolean isOnline) {
        // Lưu trạng thái online/offline vào CSDL nếu cần
        Optional<UserPresence> presenceOpt = userPresenceRepository.findById(userId);
        UserPresence presence = presenceOpt.orElseGet(() -> UserPresence.builder()
                .userId(userId)
                .build());
        presence.setOnline(isOnline);
        if (!isOnline) {
            presence.setLastActive(Instant.now());
        }
        userPresenceRepository.save(presence);
    }
    public Set<String> getOnlineUsers() {
        // Lấy tất cả userId đang online từ Redis
        // Cách đơn giản là quét tất cả key "presence:sessions:*" và lấy những key có sessionCount > 0
        Set<String> onlineUserIds = new HashSet<>();
        Set<String> keys = redisTemplate.keys("presence:sessions:*");
        if (keys != null) {
            for (String key : keys) {
                Long sessionCount = redisTemplate.opsForSet().size(key);
                if (sessionCount != null && sessionCount > 0) {
                    String userIdStr = key.substring("presence:sessions:".length());
                    onlineUserIds.add(userIdStr);
                }
            }
        }
        return onlineUserIds;
    }
    /**
     * Được gọi MỘT LẦN khi client kết nối WebSocket (ví dụ: từ WebSocketConnectHandler).
     */


    public void handleConnection(UUID userId, String sessionId) {
        log.info("User {} connected with session {}", userId, sessionId);
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);
        
        // Thêm session vào Set. `isFirstSession` = true nếu đây là session đầu tiên
        boolean isFirstSession = redisTemplate.opsForSet().add(sessionsKey, sessionId) == 1;

        if (isFirstSession) {
            // Là session đầu tiên -> Gửi event "ONLINE"
            log.info("User {} is now ONLINE (first session)", userId);
            sendOnlineStatusEvent(userId, true);
        }
        
        // Luôn đặt nhịp tim đầu tiên
        handleHeartbeat(userId, sessionId);
    }

    /**
     * Được gọi MỖI 30 GIÂY từ client (Frontend).
     * Đây là "nhịp tim" (ping) để giữ cho session còn sống.
     */
    public void handleHeartbeat(UUID userId, String sessionId) {
        log.debug("Heartbeat received for user {}, session {}", userId, sessionId);
        String heartbeatKey = String.format(SESSION_HEARTBEAT_KEY, userId, sessionId);
        
        // Gia hạn key thêm 60 giây. Nếu client ngừng ping, key này sẽ hết hạn.
        redisTemplate.opsForValue().set(heartbeatKey, "1", Duration.ofSeconds(60));
    }

    /**
     * Được gọi khi key "presence:hb:..." HẾT HẠN (từ RedisKeyExpirationListener).
     * Đây là logic xử lý "ngắt kết nối bẩn".
     */
    public void handleExpiredSession(UUID userId, String sessionId) {
        log.warn("Heartbeat expired for user {}, session {}", userId, sessionId);
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);

        // Xóa session khỏi Set. `isLastSession` = true nếu không còn session nào
        Long remainingSessions = redisTemplate.opsForSet().remove(sessionsKey, sessionId);
        
        if (remainingSessions != null && remainingSessions == 0) {
            // Là session cuối cùng -> Gửi event "OFFLINE"
            log.warn("User {} is now OFFLINE (last session expired)", userId);
            sendOnlineStatusEvent(userId, false);
        }
    }
    
    /**
     * Được gọi định kỳ (ví dụ: mỗi 5 phút) để làm mới các session vẫn còn hoạt động.
     * Nếu session không còn hoạt động, nó sẽ bị loại bỏ.
     */

     public void refreshUserSession(UUID userId, String sessionId) {
        log.debug("Refreshing session for user {}, session {}", userId, sessionId);
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);
        
        // Kiểm tra xem session có tồn tại không
        Boolean isMember = redisTemplate.opsForSet().isMember(sessionsKey, sessionId);
        if (Boolean.TRUE.equals(isMember)) {
            // Gia hạn nhịp tim
            handleHeartbeat(userId, sessionId);
        } else {
            log.warn("Session {} for user {} not found during refresh", sessionId, userId);
        }
    }
    /**
     * Được gọi khi user bấm "Logout" (ngắt kết nối sạch).
     */
    public void handleLogout(UUID userId, String sessionId) {
        log.info("User {} clean logout from session {}", userId, sessionId);
        String heartbeatKey = String.format(SESSION_HEARTBEAT_KEY, userId, sessionId);
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);

        // 1. Chủ động xóa key heartbeat
        redisTemplate.delete(heartbeatKey); 
        
        // 2. Xóa session
        Long remainingSessions = redisTemplate.opsForSet().remove(sessionsKey, sessionId);
        
        if (remainingSessions != null && remainingSessions == 0) {
            // Là session cuối cùng -> Gửi event "OFFLINE"
            log.info("User {} is now OFFLINE (clean logout)", userId);
            sendOnlineStatusEvent(userId, false);
        }
    }

    private void sendOnlineStatusEvent(UUID userId, boolean isOnline) {
        OnlineStatusEvent event = OnlineStatusEvent.builder()
                .userId(userId)
                .online(isOnline)
                .timestamp(Instant.now())
                .build();
        kafkaEventProducer.sendOnlineStatusEvent(event);
    }

    // ----------------------------------------------------------------
    // 2. QUẢN LÝ THEO DÕI (SUBSCRIPTION) - CÓ SYNC
    // ----------------------------------------------------------------

    /**
     * Đồng bộ hóa (Sync) danh sách theo dõi.
     * Được gọi khi user online và gửi danh sách bạn bè/chung phòng.
     */
    public void syncSubscriptions(UUID subscriberId, List<UUID> targetUserIds) {
        String mySubsKey = String.format(MY_SUBSCRIPTIONS_KEY, subscriberId);
        Set<String> currentSubIdsStr = redisTemplate.opsForSet().members(mySubsKey);
        if (currentSubIdsStr == null) currentSubIdsStr = Collections.emptySet();
        
        Set<String> newSubIdsStr = targetUserIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());

        Set<String> toAdd = new HashSet<>(newSubIdsStr);
        toAdd.removeAll(currentSubIdsStr); // Lấy những ID mới

        Set<String> toRemove = new HashSet<>(currentSubIdsStr);
        toRemove.removeAll(newSubIdsStr); // Lấy những ID cũ (đã hủy kết bạn)

        // Dùng Pipelining để chạy 2 hàm Unsub và Sub
        redisTemplate.executePipelined((RedisConnection connection) -> {
            
            // Dọn dẹp 1: Hủy theo dõi 100 người đã hủy kết bạn
            if (!toRemove.isEmpty()) {
                log.debug("User {} unsubscribing from {} users", subscriberId, toRemove.size());
                for (String targetIdStr : toRemove) {
                    // Xóa TÔI khỏi danh sách watcher của HỌ
                    String theirWatchersKey = String.format(MY_WATCHERS_KEY, targetIdStr);
                    connection.sRem(theirWatchersKey.getBytes(), subscriberId.toString().getBytes());
                }
                // Xóa HỌ khỏi danh sách TÔI theo dõi
                connection.sRem(mySubsKey.getBytes(), toRemove.toArray(new byte[0][0]));
            }

            // Thêm 2: Theo dõi những người mới
            if (!toAdd.isEmpty()) {
                 log.debug("User {} subscribing to {} new users", subscriberId, toAdd.size());
                for (String targetIdStr : toAdd) {
                    // Thêm TÔI vào danh sách watcher của HỌ
                    String theirWatchersKey = String.format(MY_WATCHERS_KEY, targetIdStr);
                    connection.sAdd(theirWatchersKey.getBytes(), subscriberId.toString().getBytes());
                }
                // Thêm HỌ vào danh sách TÔI theo dõi
                connection.sAdd(mySubsKey.getBytes(), toAdd.toArray(new byte[0][0]));
            }
            
            // Luôn gia hạn key chính (24 giờ)
            connection.expire(mySubsKey.getBytes(), Duration.ofHours(24).toSeconds());
            return null;
        });
    }

    /**
     * Lấy danh sách ID của những người đang theo dõi TÔI.
     */
    public Set<UUID> getWatchers(UUID userId) {
        String watchersKey = String.format(MY_WATCHERS_KEY, userId);
        Set<String> watcherIdsStr = redisTemplate.opsForSet().members(watchersKey);
        
        if (watcherIdsStr == null) return Collections.emptySet();
        
        return watcherIdsStr.stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    // ----------------------------------------------------------------
    // 3. LẤY TRẠNG THÁI (PULL) - TỐI ƯU PIPELINE
    // ----------------------------------------------------------------

    /**
     * Lấy trạng thái của NHIỀU user (ví dụ: 200 người) trong 1 truy vấn.
     * Giải quyết vấn đề N+1.
     */
    public Map<UUID, UserPresenceResponse> getBatchPresence(List<UUID> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }

        Map<UUID, UserPresenceResponse> resultMap = new HashMap<>();

        try {
            // 1. LẤY TRẠNG THÁI ONLINE (TỪ REDIS)
            // Dùng Pipelining để gửi N lệnh SCARD (đếm số session) 1 lúc
            List<Object> pipelineSessionCounts = redisTemplate.executePipelined(
                (RedisConnection connection) -> {
                    for (UUID userId : userIds) {
                        String userSessionsKey = String.format(USER_SESSIONS_KEY, userId);
                        connection.sCard(userSessionsKey.getBytes(StandardCharsets.UTF_8));
                    }
                    return null;
                });

            // 2. LẤY QUYỀN RIÊNG TƯ & LAST ACTIVE (TỪ CASSANDRA)
            // 1 truy vấn CSDL duy nhất
            Map<UUID, UserPresence> presenceMap = userPresenceRepository.findByUserIdIn(userIds)
                    .stream()
                    .collect(Collectors.toMap(UserPresence::getUserId, p -> p));

            // 3. TỔNG HỢP KẾT QUẢ
            for (int i = 0; i < userIds.size(); i++) {
                UUID userId = userIds.get(i);
                long sessionCount = (Long) pipelineSessionCounts.get(i);
                boolean isOnline = (sessionCount > 0);

                UserPresence presenceData = presenceMap.get(userId);
                String status = "OFFLINE";
                Instant lastActive = (presenceData != null) ? presenceData.getLastActive() : null;

                // Áp dụng logic Quyền riêng tư
                if (presenceData != null && presenceData.isHidden()) {
                    isOnline = false; // "Tắt hoạt động" -> luôn hiện OFFLINE
                }
                
                // TODO: Thêm logic "FRIENDS_ONLY"
                // Bạn cần 1 cách để check (userId, requesterId) có phải là bạn không
                
                if (isOnline) {
                    status = "ONLINE";
                    lastActive = null; // Đang online thì không hiển thị lastActive
                }

                resultMap.put(userId, UserPresenceResponse.builder()
                        .userId(userId)
                        .isOnline(isOnline)
                        .status(status)
                        .lastSeen(lastActive)
                        .build());
            }

        } catch (Exception e) {
            log.error("Lỗi khi lấy batch presence (pipeline)", e);
        }
        
        return resultMap;
    }
}