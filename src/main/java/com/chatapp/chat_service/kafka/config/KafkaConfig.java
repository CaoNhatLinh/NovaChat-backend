package com.chatapp.chat_service.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // =========================================================================
    // == Producer Configuration (Đã gộp)
    // =========================================================================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // GIẢI PHÁP 2: Tự động thêm thông tin kiểu (type info) vào header
        // Tốt hơn nhiều so với việc dùng TYPE_MAPPINGS
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        // Cấu hình Producer an toàn (Idempotence)
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); 
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // =========================================================================
    // == Common Consumer & Error Handler Beans
    // =========================================================================

    /**
     * Cấu hình Error Handler chung cho các Listener.
     * Sẽ thử lại 3 lần, mỗi lần cách nhau 1 giây.
     */
    @Bean
    public DefaultErrorHandler commonKafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 3L) // 1 giây delay, thử lại tối đa 3 lần
        );
        errorHandler.setLogLevel(KafkaException.Level.ERROR);
        
        // Không thử lại nếu lỗi do Deserialization (tin nhắn bị hỏng)
        errorHandler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class,
                org.apache.kafka.common.errors.RecordDeserializationException.class,
                IllegalArgumentException.class
        );
        return errorHandler;
    }

    /**
     * Thuộc tính Consumer chung
     */
    private Map<String, Object> consumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // BẮT BUỘC: Luôn tắt auto-commit khi dùng Manual Ack
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); 
        
        // Đọc thông tin kiểu (type info) từ header mà Producer đã gửi
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true); 
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.chatapp.chat_service.*, java.util, java.lang, java.time");
        
        // Cấu hình cơ bản
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        
        return props;
    }

    // =========================================================================
    // == Listener Factory 1: MessageEvent
    // =========================================================================
    @Bean
    public ConsumerFactory<String, Object> messageEventConsumerFactory() {
        Map<String, Object> props = consumerProps("chat-service-messages");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> messageEventListenerFactory(
            ConsumerFactory<String, Object> messageEventConsumerFactory,
            DefaultErrorHandler commonKafkaErrorHandler) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(messageEventConsumerFactory);
        factory.setCommonErrorHandler(commonKafkaErrorHandler); // Áp dụng Error Handler
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // Bật Manual Ack
        return factory;
    }

    // =========================================================================
    // == Listener Factory 2: OnlineStatusEvent
    // =========================================================================
    @Bean
    public ConsumerFactory<String, Object> onlineStatusEventConsumerFactory() {
        Map<String, Object> props = consumerProps("chat-service-online-status");
        
        // CRITICAL: Bắt đầu từ offset "mới nhất" để tránh xử lý sự kiện online/offline cũ
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        // Đảm bảo xử lý tuần tự từng tin nhắn
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> onlineStatusEventListenerFactory(
            ConsumerFactory<String, Object> onlineStatusEventConsumerFactory,
            DefaultErrorHandler commonKafkaErrorHandler) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(onlineStatusEventConsumerFactory);
        factory.setCommonErrorHandler(commonKafkaErrorHandler); // Áp dụng Error Handler
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // Bật Manual Ack
        factory.setConcurrency(1); // Chỉ 1 consumer thread để đảm bảo thứ tự
        return factory;
    }

    // =========================================================================
    // == Listener Factory 3: FriendshipStatusEvent
    // =========================================================================
    @Bean
    public ConsumerFactory<String, Object> friendshipStatusConsumerFactory() {
        Map<String, Object> props = consumerProps("chat-service-friendship");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> friendshipStatusListenerFactory(
            ConsumerFactory<String, Object> friendshipStatusConsumerFactory,
            DefaultErrorHandler commonKafkaErrorHandler) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(friendshipStatusConsumerFactory);
        factory.setCommonErrorHandler(commonKafkaErrorHandler); // Áp dụng Error Handler
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // Bật Manual Ack
        return factory;
    }
    
    // =========================================================================
    // == Listener Factory 4: TypingEvent (Thêm vào cho nhất quán)
    // =========================================================================
     @Bean
    public ConsumerFactory<String, Object> typingEventConsumerFactory() {
        Map<String, Object> props = consumerProps("chat-service-typing");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> typingEventListenerFactory(
            ConsumerFactory<String, Object> typingEventConsumerFactory,
            DefaultErrorHandler commonKafkaErrorHandler) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(typingEventConsumerFactory);
        factory.setCommonErrorHandler(commonKafkaErrorHandler); // Áp dụng Error Handler
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // Bật Manual Ack
        return factory;
    }
}