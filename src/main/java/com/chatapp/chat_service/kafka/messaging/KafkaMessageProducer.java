package com.chatapp.chat_service.kafka.messaging;


import com.chatapp.chat_service.websocket.event.MessageReactionEvent;
import com.chatapp.chat_service.websocket.event.OnlineStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class KafkaMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMessageProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessageEvent(Object event) {
        kafkaTemplate.send("message-topic", event);
    }


    public void sendOnlineStatusEvent(Object event) {
        if (event instanceof OnlineStatusEvent) {
            OnlineStatusEvent statusEvent = (OnlineStatusEvent) event;
            UUID userId = statusEvent.getUserId();
            String userKey = userId != null ? userId.toString() : "unknown";
            
            log.info("=== KAFKA PRODUCER === Sending OnlineStatusEvent: user={}, online={}, timestamp={}", 
                    userKey, statusEvent.isOnline(), statusEvent.getTimestamp());
            
            // Use userId as partition key to ensure ordered processing per user
            kafkaTemplate.send("online-status-topic", userKey, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("OnlineStatusEvent sent successfully for user: {}", userKey);
                    } else {
                        log.error("Failed to send OnlineStatusEvent for user {}: {}", userKey, ex.getMessage());
                    }
                });
        } else {
            kafkaTemplate.send("online-status-topic", event);
        }
    }

    public void sendReactionEvent(MessageReactionEvent event) {
        kafkaTemplate.send("message-reaction-topic", event);
    }

    public void sendReadReceiptEvent(Object event) {
        kafkaTemplate.send("message-read-topic", event);
    }

    public void sendPinEvent(Object event) {
        kafkaTemplate.send("message-pin-topic", event);
    }

    public void sendAttachmentEvent(Object event) {
        kafkaTemplate.send("message-attachment-topic", event);
    }

    public void sendNotificationEvent(Object event) {
        kafkaTemplate.send("notification-topic", event);
    }
}


