package com.chatapp.chat_service.service;


import com.chatapp.chat_service.model.dto.MessageSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationCacheService {

    private static final String CACHE_KEY_PREFIX = "conv:last_msg:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void cacheLastMessage(UUID conversationId, MessageSummary summary) {
        try {
            String key = CACHE_KEY_PREFIX + conversationId;
            String value = objectMapper.writeValueAsString(summary);
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message summary for caching", e);
        }
    }

    public Optional<MessageSummary> getCachedLastMessage(UUID conversationId) {
        try {
            String key = CACHE_KEY_PREFIX + conversationId;
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                String jsonValue = value.toString();
                return Optional.of(objectMapper.readValue(jsonValue, MessageSummary.class));
            }
        } catch (Exception e) {
            log.error("Failed to deserialize cached message summary", e);
        }
        return Optional.empty();
    }

    public void evictCache(UUID conversationId) {
        String key = CACHE_KEY_PREFIX + conversationId;
        redisTemplate.delete(key);
    }

    /**
     * Xóa tất cả cache liên quan đến conversation
     *
     * @param conversationId ID của conversation
     */
    public void clearConversationCache(String conversationId) {
        try {
            // Xóa private conversation cache
            String dmKey = "dm_conversation:" + conversationId;
            redisTemplate.delete(dmKey);
            log.debug("Cleared DM conversation cache: {}", dmKey);

            // Xóa conversation info cache
            String convKey = "conversation:" + conversationId;
            redisTemplate.delete(convKey);
            log.debug("Cleared conversation cache: {}", convKey);

            // Xóa conversation members cache
            String membersKey = "conversation_members:" + conversationId;
            redisTemplate.delete(membersKey);
            log.debug("Cleared conversation members cache: {}", membersKey);

            // Xóa message cache cho conversation này
            String messagePattern = "message:" + conversationId + ":*";
            Set<String> messageKeys = redisTemplate.keys(messagePattern);
            if (messageKeys != null && !messageKeys.isEmpty()) {
                redisTemplate.delete(messageKeys);
                log.debug("Cleared {} message cache entries for conversation: {}", messageKeys.size(), conversationId);
            }

            // Xóa last message cache
            evictCache(UUID.fromString(conversationId));

        } catch (Exception e) {
            log.error("Error clearing conversation cache for {}: {}", conversationId, e.getMessage(), e);
        }
    }

    /**
     * Xóa cache user conversations cho một user
     *
     * @param userId ID của user
     */
    public void clearUserConversationsCache(String userId) {
        try {
            String pattern = "user_conversations:" + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cleared {} user conversations cache entries for user: {}", keys.size(), userId);
            }
        } catch (Exception e) {
            log.error("Error clearing user conversations cache for {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Xóa cache cho typing users trong conversation
     *
     * @param conversationId ID của conversation
     */
    public void clearTypingCache(String conversationId) {
        try {
            String typingKey = "typing_users:" + conversationId;
            redisTemplate.delete(typingKey);
            log.debug("Cleared typing cache: {}", typingKey);
        } catch (Exception e) {
            log.error("Error clearing typing cache for {}: {}", conversationId, e.getMessage(), e);
        }
    }

    /**
     * Xóa tất cả cache theo pattern
     *
     * @param pattern Pattern để tìm cache keys
     * @return Số lượng entries đã xóa
     */
    public long clearCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                long deletedCount = deleted != null ? deleted : 0;
                log.debug("Cleared {} cache entries for pattern: {}", deletedCount, pattern);
                return deletedCount;
            }
            return 0;
        } catch (Exception e) {
            log.error("Error clearing cache by pattern {}: {}", pattern, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Kiểm tra xem có bao nhiêu cache entries cho một pattern
     *
     * @param pattern Pattern để tìm
     * @return Số lượng cache entries
     */
    public long countCacheEntries(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Error counting cache entries for pattern {}: {}", pattern, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Health check cho cache
     *
     * @return true nếu Redis connection OK
     */
    public boolean isHealthy() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}