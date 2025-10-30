package com.chatapp.chat_service.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Slf4j
public class TopicConfiguration {

    // CHÚ Ý: replicas(1) chỉ dành cho local. Production nên là 3.
    private final int REPLICAS = 1;

    @Bean
    public NewTopic onlineStatusTopic() {
        log.info("Creating online-status-topic with 1 partition for ordered processing");
        return TopicBuilder.name("online-status-topic")
                .partitions(1)  // Rất tốt! Giữ nguyên 1 partition để đảm bảo thứ tự
                .replicas(REPLICAS)
                .config("retention.ms", "3600000") // 1 giờ
                .build();
    }

    @Bean
    public NewTopic messageTopic() {
        return TopicBuilder.name("message-topic")
                .partitions(3)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic typingEventsTopic() {
        return TopicBuilder.name("typing-events-topic")
                .partitions(2)
                .replicas(REPLICAS)
                .config("retention.ms", "300000") // 5 phút
                .build();
    }

    @Bean
    public NewTopic friendRequestTopic() {
        return TopicBuilder.name("friend-requests-topic")
                .partitions(1)
                .replicas(REPLICAS)
                .build();
    }
    
    @Bean
    public NewTopic friendshipStatusTopic() {
        return TopicBuilder.name("friendship-status-events")
                .partitions(1)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic messageReactionTopic() {
        return TopicBuilder.name("message-reaction-topic")
                .partitions(2)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic messageReadTopic() {
        return TopicBuilder.name("message-read-topic")
                .partitions(2)
                .replicas(REPLICAS)
                .build();
    }
    
    @Bean
    public NewTopic messagePinTopic() {
        return TopicBuilder.name("message-pin-topic")
                .partitions(2)
                .replicas(REPLICAS)
                .build();
    }
    
    @Bean
    public NewTopic messageAttachmentTopic() {
        return TopicBuilder.name("message-attachment-topic")
                .partitions(2)
                .replicas(REPLICAS)
                .build();
    }
    
    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name("notification-topic")
                .partitions(1)
                .replicas(REPLICAS)
                .build();
    }
}