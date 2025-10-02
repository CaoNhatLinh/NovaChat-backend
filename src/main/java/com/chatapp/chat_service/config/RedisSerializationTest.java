package com.chatapp.chat_service.config;

import com.chatapp.chat_service.model.dto.MessageSummary;
import com.chatapp.chat_service.model.entity.Conversation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Order(100) // Run after other CommandLineRunners
@Slf4j
public class RedisSerializationTest implements CommandLineRunner {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Testing Redis serialization with Java 8 time types...");
            
            // Create a test message summary
            MessageSummary testMessageSummary = MessageSummary.builder()
                    .messageId(UUID.randomUUID())
                    .senderId(UUID.randomUUID())
                    .content("Test message content")
                    .createdAt(Instant.now())
                    .build();
            
            // Create a test conversation with MessageSummary
            Conversation testConversation = Conversation.builder()
                    .conversationId(UUID.randomUUID())
                    .name("Test Conversation")
                    .type("GROUP")
                    .created_at(Instant.now())
                    .updated_at(Instant.now())
                    .last_message(testMessageSummary)
                    .created_by(UUID.randomUUID())
                    .is_deleted(false)
                    .build();

            String testKey = "test:conversation:" + testConversation.getConversationId();

            // Test serialization
            redisTemplate.opsForValue().set(testKey, testConversation);
            log.info("✅ Successfully serialized conversation with MessageSummary to Redis");

            // Test deserialization
            Object retrieved = redisTemplate.opsForValue().get(testKey);
            if (retrieved instanceof Conversation) {
                Conversation retrievedConversation = (Conversation) retrieved;
                log.info("✅ Successfully deserialized conversation from Redis");
                log.info("   - Created At: {}", retrievedConversation.getCreated_at());
                log.info("   - Updated At: {}", retrievedConversation.getUpdated_at());
                log.info("   - Last Message: {}", retrievedConversation.getLast_message());
                if (retrievedConversation.getLast_message() != null) {
                    log.info("   - Last Message Created At: {}", retrievedConversation.getLast_message().getCreatedAt());
                    log.info("   - Last Message Content: {}", retrievedConversation.getLast_message().getContent());
                }
            } else {
                log.warn("⚠️ Retrieved object is not a Conversation instance: {}", 
                         retrieved != null ? retrieved.getClass() : "null");
            }

            // Clean up test data
            redisTemplate.delete(testKey);
            log.info("🧹 Cleaned up test data from Redis");
            
        } catch (Exception e) {
            log.error("❌ Redis serialization test failed:", e);
        }
    }
}
