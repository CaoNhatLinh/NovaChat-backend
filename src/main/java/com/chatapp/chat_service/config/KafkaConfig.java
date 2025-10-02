package com.chatapp.chat_service.config;

import com.chatapp.chat_service.websocket.event.FriendshipStatusEvent;
import com.chatapp.chat_service.websocket.event.MessageEvent;
import com.chatapp.chat_service.websocket.event.OnlineStatusEvent;
import com.chatapp.chat_service.websocket.event.TypingEvent;
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
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Common consumer properties to ensure consistency across all consumers
     */
    private Map<String, Object> getCommonConsumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual acknowledgment
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        return props;
    }

    /**
     * Common JSON deserializer properties
     */
    private void addJsonDeserializerProps(Map<String, Object> props, String typeMapping, String defaultType) {
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.chatapp.*,java.util,java.lang,java.time");
        props.put(JsonDeserializer.TYPE_MAPPINGS, typeMapping);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, defaultType);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    }

    // Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.TYPE_MAPPINGS,
                "friendRequest:com.chatapp.chat_service.websocket.event.FriendRequestEvent," +
                        "friendshipStatus:com.chatapp.chat_service.websocket.event.FriendshipStatusEvent");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, FriendshipStatusEvent> friendshipStatusConsumerFactory() {
        Map<String, Object> props = getCommonConsumerProps("chat-service-friendship");
        
        // Configure ErrorHandlingDeserializer for robust error handling
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        addJsonDeserializerProps(props, 
                "friendshipStatus:com.chatapp.chat_service.websocket.event.FriendshipStatusEvent",
                "com.chatapp.chat_service.websocket.event.FriendshipStatusEvent");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FriendshipStatusEvent>
    friendshipStatusListenerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, FriendshipStatusEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(friendshipStatusConsumerFactory());

        // Configure robust error handling
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 3L) // Retry 3 times with 1 second intervals
        );
        errorHandler.setLogLevel(KafkaException.Level.ERROR);
        
        // Add custom error handler logic - don't retry on deserialization errors
        errorHandler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class,
                org.apache.kafka.common.errors.RecordDeserializationException.class,
                IllegalArgumentException.class
        );
        
        factory.setCommonErrorHandler(errorHandler);
        
        // Enable manual acknowledgment for better control
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Enable batch processing for better performance
        factory.setBatchListener(false);

        return factory;
    }

    @Bean
    public ProducerFactory<String, Object> messageProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
    @Bean
    public ConsumerFactory<String, MessageEvent> messageConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-service-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(MessageEvent.class)
        );
    }
    @Bean
    public ConsumerFactory<String, MessageEvent> messageEventConsumerFactory() {
        Map<String, Object> props = getCommonConsumerProps("chat-service-messages");
        addJsonDeserializerProps(props, "messageEvent:com.chatapp.chat_service.websocket.event.MessageEvent", "com.chatapp.chat_service.websocket.event.MessageEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MessageEvent> messageEventListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MessageEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(messageEventConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, TypingEvent> typingEventConsumerFactory() {
        Map<String, Object> props = getCommonConsumerProps("chat-service-typing");
        addJsonDeserializerProps(props, "typingEvent:com.chatapp.chat_service.websocket.event.TypingEvent", "com.chatapp.chat_service.websocket.event.TypingEvent");
        return new DefaultKafkaConsumerFactory<>(props);
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TypingEvent> typingEventListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TypingEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(typingEventConsumerFactory());
        return factory;
    }
    @Bean
    public ConsumerFactory<String, OnlineStatusEvent> onlineStatusEventConsumerFactory() {
        Map<String, Object> props = getCommonConsumerProps("chat-service-online-status");
        
        // Configure ErrorHandlingDeserializer for robust error handling
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        
        // CRITICAL: Start from latest to avoid stale events on restart
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        // Critical: Ensure only one instance processes each message
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // Faster session timeout for quicker rebalancing
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        addJsonDeserializerProps(props, "onlineStatus:com.chatapp.chat_service.websocket.event.OnlineStatusEvent", "com.chatapp.chat_service.websocket.event.OnlineStatusEvent");
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OnlineStatusEvent> onlineStatusEventListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OnlineStatusEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(onlineStatusEventConsumerFactory());
        
        // Configure robust error handling
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 2L) // Retry 2 times with 1 second intervals
        );
        errorHandler.setLogLevel(KafkaException.Level.WARN);
        
        // Add custom error handler logic - don't retry on deserialization errors
        errorHandler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class,
                org.apache.kafka.common.errors.RecordDeserializationException.class,
                IllegalArgumentException.class
        );
        
        factory.setCommonErrorHandler(errorHandler);
        
        // Critical: Set concurrency to 1 to prevent duplicate processing
        factory.setConcurrency(1);
        
        // Enable manual acknowledgment for better control
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        return factory;
    }
}