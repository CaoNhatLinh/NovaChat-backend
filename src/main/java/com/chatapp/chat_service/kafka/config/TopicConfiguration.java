package com.chatapp.chat_service.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Slf4j
public class TopicConfiguration {

    /**
     * Online status topic with single partition to ensure ordered processing
     * This prevents race conditions and duplicate events
     */
    @Bean
    public NewTopic onlineStatusTopic() {
        log.info("Creating online-status-topic with 1 partition for ordered processing");
        return TopicBuilder.name("online-status-topic")
                .partitions(1)  // Single partition ensures order
                .replicas(1)    // Single replica for development
                .config("retention.ms", "3600000")   // 1 hour retention (was 24h)
                .config("segment.ms", "600000")      // 10 minute segments (was 1h)
                .config("cleanup.policy", "delete")  // Delete old messages
                .config("delete.retention.ms", "300000")  // 5 minutes delete retention
                .build();
    }

    /**
     * Message topic with multiple partitions for high throughput
     */
    @Bean
    public NewTopic messageTopic() {
        return TopicBuilder.name("message-topic")
                .partitions(3)   // Multiple partitions for throughput
                .replicas(1)
                .build();
    }

    /**
     * Typing events topic - can have multiple partitions as order is less critical
     */
    @Bean
    public NewTopic typingEventsTopic() {
        return TopicBuilder.name("typing-events-topic")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "300000")  // 5 minutes retention
                .build();
    }
}
