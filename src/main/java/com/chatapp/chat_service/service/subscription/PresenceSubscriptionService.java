//package com.chatapp.chat_service.service.subscription;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.Set;
//import java.util.UUID;
//import java.util.HashSet;
//import java.util.stream.Collectors;
//
///**
// * Service để quản lý subscription cho presence tracking
// * Cho phép user subscribe/unsubscribe để nhận updates về online status
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PresenceSubscriptionService {
//
//    private final RedisTemplate<String, String> redisTemplate;
//
//    private static final String SUBSCRIPTION_KEY_PREFIX = "presence:subscription:";
//    private static final String SUBSCRIBERS_KEY_PREFIX = "presence:subscribers:";
//    private static final int SUBSCRIPTION_TTL_SECONDS = 3600; // 1 hour
//
//    /**
//     * Subscribe user để nhận presence updates từ target user
//     * @param subscriberId ID của user muốn subscribe
//     * @param targetUserId ID của user được subscribe
//     */
//    public void subscribeToPresence(UUID subscriberId, UUID targetUserId) {
//        try {
//            // Add target user to subscriber's subscription list
//            String subscriptionKey = SUBSCRIPTION_KEY_PREFIX + subscriberId;
//            redisTemplate.opsForSet().add(subscriptionKey, targetUserId.toString());
//            redisTemplate.expire(subscriptionKey, Duration.ofSeconds(SUBSCRIPTION_TTL_SECONDS));
//
//            // Add subscriber to target user's subscribers list
//            String subscribersKey = SUBSCRIBERS_KEY_PREFIX + targetUserId;
//            redisTemplate.opsForSet().add(subscribersKey, subscriberId.toString());
//            redisTemplate.expire(subscribersKey, Duration.ofSeconds(SUBSCRIPTION_TTL_SECONDS));
//
//            log.debug("User {} subscribed to presence updates from user {}", subscriberId, targetUserId);
//        } catch (Exception e) {
//            log.error("Error subscribing user {} to presence of user {}", subscriberId, targetUserId, e);
//        }
//    }
//
//    /**
//     * Unsubscribe user khỏi presence updates của target user
//     * @param subscriberId ID của user muốn unsubscribe
//     * @param targetUserId ID của user được unsubscribe
//     */
//    public void unsubscribeFromPresence(UUID subscriberId, UUID targetUserId) {
//        try {
//            // Remove target user from subscriber's subscription list
//            String subscriptionKey = SUBSCRIPTION_KEY_PREFIX + subscriberId;
//            redisTemplate.opsForSet().remove(subscriptionKey, targetUserId.toString());
//
//            // Remove subscriber from target user's subscribers list
//            String subscribersKey = SUBSCRIBERS_KEY_PREFIX + targetUserId;
//            redisTemplate.opsForSet().remove(subscribersKey, subscriberId.toString());
//
//            log.debug("User {} unsubscribed from presence updates of user {}", subscriberId, targetUserId);
//        } catch (Exception e) {
//            log.error("Error unsubscribing user {} from presence of user {}", subscriberId, targetUserId, e);
//        }
//    }
//
//    /**
//     * Lấy danh sách các user mà subscriber đang subscribe
//     * @param subscriberId ID của subscriber
//     * @return Set các user ID đang được subscribe
//     */
//    public Set<UUID> getSubscriptions(UUID subscriberId) {
//        try {
//            String subscriptionKey = SUBSCRIPTION_KEY_PREFIX + subscriberId;
//            Set<String> subscriptions = redisTemplate.opsForSet().members(subscriptionKey);
//
//            if (subscriptions == null || subscriptions.isEmpty()) {
//                return new HashSet<>();
//            }
//
//            return subscriptions.stream()
//                    .map(UUID::fromString)
//                    .collect(Collectors.toSet());
//        } catch (Exception e) {
//            log.error("Error getting subscriptions for user {}", subscriberId, e);
//            return new HashSet<>();
//        }
//    }
//
//    /**
//     * Lấy danh sách các user đang subscribe target user
//     * @param targetUserId ID của target user
//     * @return Set các subscriber ID
//     */
//    public Set<UUID> getSubscribers(UUID targetUserId) {
//        try {
//            String subscribersKey = SUBSCRIBERS_KEY_PREFIX + targetUserId;
//            Set<String> subscribers = redisTemplate.opsForSet().members(subscribersKey);
//
//            if (subscribers == null || subscribers.isEmpty()) {
//                return new HashSet<>();
//            }
//
//            return subscribers.stream()
//                    .map(UUID::fromString)
//                    .collect(Collectors.toSet());
//        } catch (Exception e) {
//            log.error("Error getting subscribers for user {}", targetUserId, e);
//            return new HashSet<>();
//        }
//    }
//
//    /**
//     * Kiểm tra user có đang subscribe target user không
//     * @param subscriberId ID của subscriber
//     * @param targetUserId ID của target user
//     * @return true nếu đang subscribe
//     */
//    public boolean isSubscribed(UUID subscriberId, UUID targetUserId) {
//        try {
//            String subscriptionKey = SUBSCRIPTION_KEY_PREFIX + subscriberId;
//            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(subscriptionKey, targetUserId.toString()));
//        } catch (Exception e) {
//            log.error("Error checking subscription status for user {} to user {}", subscriberId, targetUserId, e);
//            return false;
//        }
//    }
//
//    /**
//     * Cleanup expired subscriptions
//     */
//    public void cleanupExpiredSubscriptions() {
//        try {
//            // This method can be called periodically to clean up expired subscriptions
//            // Redis TTL will handle most of the cleanup automatically
//            log.debug("Cleaned up expired subscriptions");
//        } catch (Exception e) {
//            log.error("Error cleaning up expired subscriptions", e);
//        }
//    }
//
//    /**
//     * Refresh subscription TTL
//     * @param subscriberId ID của subscriber
//     * @param targetUserId ID của target user
//     */
//    public void refreshSubscription(UUID subscriberId, UUID targetUserId) {
//        try {
//            String subscriptionKey = SUBSCRIPTION_KEY_PREFIX + subscriberId;
//            String subscribersKey = SUBSCRIBERS_KEY_PREFIX + targetUserId;
//
//            // Refresh TTL if keys exist
//            if (Boolean.TRUE.equals(redisTemplate.hasKey(subscriptionKey))) {
//                redisTemplate.expire(subscriptionKey, Duration.ofSeconds(SUBSCRIPTION_TTL_SECONDS));
//            }
//
//            if (Boolean.TRUE.equals(redisTemplate.hasKey(subscribersKey))) {
//                redisTemplate.expire(subscribersKey, Duration.ofSeconds(SUBSCRIPTION_TTL_SECONDS));
//            }
//
//            log.debug("Refreshed subscription TTL for user {} to user {}", subscriberId, targetUserId);
//        } catch (Exception e) {
//            log.error("Error refreshing subscription TTL for user {} to user {}", subscriberId, targetUserId, e);
//        }
//    }
//}
