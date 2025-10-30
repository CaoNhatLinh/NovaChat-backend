package com.chatapp.chat_service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupCacheCleanup implements ApplicationRunner {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Kiểm tra môi trường
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevelopment = Arrays.asList(activeProfiles).contains("dev") ||
                Arrays.asList(activeProfiles).contains("development") ||
                activeProfiles.length == 0; // default profile

        // Kiểm tra property enable
        boolean isEnabled = environment.getProperty("cache.startup.clear.enabled", Boolean.class, true);

        if (!isEnabled) {
            log.info("⏭️  Cache startup cleanup is disabled");
            return;
        }

        if (!isDevelopment) {
            log.info("⏭️  Skipping cache cleanup in production environment");
            return;
        }

        try {
            log.info("🧹 Starting automatic cache cleanup on server startup (dev mode)...");

            long startTime = System.currentTimeMillis();

            clearAllApplicationCache();

            long endTime = System.currentTimeMillis();
            log.info("✅ Cache cleanup completed in {} ms", (endTime - startTime));

        } catch (Exception e) {
            log.error("❌ Error during startup cache cleanup: {}", e.getMessage(), e);
        }
    }

    private void clearAllApplicationCache() {
        String[] cachePatterns = {
                "dm_conversation:*",
                "user_conversations:*",
                "conversation:*",
                "message:*",
                "message_attachments:*",
                "message_reactions:*",
                "message_read_receipts:*",
                "pinned_messages:*",
                "poll:*",
                "poll_results:*",
                "user:*",
                "online_users:*",
                "typing_users:*",
                "conversation_members:*",
                "cache::*"
        };

        int totalDeleted = 0;

        for (String pattern : cachePatterns) {
            try {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    Long deleted = redisTemplate.delete(keys);
                    totalDeleted += deleted != null ? deleted : 0;
                    log.info("🗑️  Deleted {} entries for pattern: {}", deleted, pattern);
                }
            } catch (Exception e) {
                log.warn("Failed to clear cache pattern {}: {}", pattern, e.getMessage());
            }
        }

        log.info("🗑️  Total cache entries deleted: {}", totalDeleted);
    }
}