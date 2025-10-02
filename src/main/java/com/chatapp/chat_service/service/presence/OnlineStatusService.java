package com.chatapp.chat_service.service.presence;

import com.chatapp.chat_service.kafka.messaging.KafkaMessageProducer;
import com.chatapp.chat_service.websocket.event.OnlineStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service quản lý online status đơn giản
 * Được sử dụng cho các tính năng cần check online status nhanh
 * Khác với PresenceService có session-based tracking phức tạp
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineStatusService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaMessageProducer messageProducer;

    private static final String ONLINE_STATUS_KEY = "user:online";

    /**
     * Đặt user online
     * @param userId ID của user
     * @param sendKafkaEvent có gửi Kafka event không
     */
    public void setUserOnline(UUID userId, boolean sendKafkaEvent) {
        // Thêm user vào set online
        redisTemplate.opsForSet().add(ONLINE_STATUS_KEY, userId.toString());

        // Đặt TTL cho key (ví dụ: 5 phút)
        redisTemplate.expire(ONLINE_STATUS_KEY, 5, TimeUnit.MINUTES);

        if (sendKafkaEvent) {
            sendOnlineStatusEvent(userId, true);
        }

        log.debug("Set user {} online", userId);
    }

    /**
     * Đặt user online với Kafka event
     * @param userId ID của user
     */
    public void setUserOnline(UUID userId) {
        setUserOnline(userId, true);
    }

    /**
     * Đặt user offline
     * @param userId ID của user
     * @param sendKafkaEvent có gửi Kafka event không
     */
    public void setUserOffline(UUID userId, boolean sendKafkaEvent) {
        // Xóa user khỏi set online
        redisTemplate.opsForSet().remove(ONLINE_STATUS_KEY, userId.toString());

        if (sendKafkaEvent) {
            sendOnlineStatusEvent(userId, false);
        }

        log.debug("Set user {} offline", userId);
    }

    /**
     * Đặt user offline với Kafka event
     * @param userId ID của user
     */
    public void setUserOffline(UUID userId) {
        setUserOffline(userId, true);
    }

    /**
     * Kiểm tra user có online không
     * @param userId ID của user
     * @return true nếu user online
     */
    public boolean isUserOnline(UUID userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(ONLINE_STATUS_KEY, userId.toString())
        );
    }

    /**
     * Lấy tất cả users online
     * @return Set các user IDs online
     */
    public Set<String> getOnlineUsers() {
        return redisTemplate.opsForSet().members(ONLINE_STATUS_KEY);
    }

    /**
     * Đặt online status và gửi Kafka event
     * @param userId ID của user
     * @param isOnline trạng thái online
     */
    public void setUserOnlineStatus(UUID userId, boolean isOnline) {
        if (isOnline) {
            setUserOnline(userId, false); // Không gửi Kafka event trùng lặp
        } else {
            setUserOffline(userId, false); // Không gửi Kafka event trùng lặp
        }

        // Gửi Kafka event
        sendOnlineStatusEvent(userId, isOnline);
    }

    /**
     * Gửi Kafka event cho online status change
     * @param userId ID của user
     * @param isOnline trạng thái online
     */
    private void sendOnlineStatusEvent(UUID userId, boolean isOnline) {
        OnlineStatusEvent event = OnlineStatusEvent.builder()
                .userId(userId)
                .online(isOnline)
                .timestamp(Instant.now())
                .build();

        messageProducer.sendOnlineStatusEvent(event);
    }

    /**
     * Cleanup tất cả online status (maintenance)
     */
    public void clearAllOnlineStatus() {
        redisTemplate.delete(ONLINE_STATUS_KEY);
        log.info("Cleared all online status");
    }

    /**
     * Lấy số lượng users online
     * @return số lượng users online
     */
    public long getOnlineUserCount() {
        return redisTemplate.opsForSet().size(ONLINE_STATUS_KEY);
    }
}
