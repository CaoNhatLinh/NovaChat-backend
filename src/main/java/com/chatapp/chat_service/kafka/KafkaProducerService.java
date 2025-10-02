package com.chatapp.chat_service.kafka;


import com.chatapp.chat_service.websocket.event.FriendRequestEvent;
import com.chatapp.chat_service.websocket.event.FriendshipStatusEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class KafkaProducerService {
    private static final String FRIEND_REQUEST_TOPIC = "friend-requests";
    private static final String FRIENDSHIP_STATUS_TOPIC = "friendship-status-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendFriendRequestEvent(UUID senderId, UUID receiverId) {
        kafkaTemplate.send(FRIEND_REQUEST_TOPIC, new FriendRequestEvent(senderId, receiverId));
    }

    public void sendFriendshipStatusEvent(FriendshipStatusEvent event) {
        kafkaTemplate.send(FRIENDSHIP_STATUS_TOPIC, event);
    }
}