package com.chatapp.chat_service.kafka;

import com.chatapp.chat_service.friendship.event.FriendRequestEvent;
import com.chatapp.chat_service.friendship.event.FriendshipStatusEvent;
import com.chatapp.chat_service.message.event.MessageReactionEvent;
import com.chatapp.chat_service.presence.event.OnlineStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Tên các Topic (để ở 1 nơi, dễ quản lý)
    private static final String T_FRIEND_REQUEST = "friend-requests-topic";
    private static final String T_FRIENDSHIP_STATUS = "friendship-status-events";
    private static final String T_ONLINE_STATUS = "online-status-topic";
    private static final String T_MESSAGE = "message-topic";
    private static final String T_MESSAGE_REACTION = "message-reaction-topic";
    private static final String T_MESSAGE_READ = "message-read-topic";
    private static final String T_MESSAGE_PIN = "message-pin-topic";
    private static final String T_MESSAGE_ATTACHMENT = "message-attachment-topic";
    private static final String T_NOTIFICATION = "notification-topic";

    // == Friendship Events ==
    
    public void sendFriendRequestEvent(UUID senderId, UUID receiverId) {
        kafkaTemplate.send(T_FRIEND_REQUEST, new FriendRequestEvent(senderId, receiverId));
    }

    public void sendFriendshipStatusEvent(FriendshipStatusEvent event) {
        kafkaTemplate.send(T_FRIENDSHIP_STATUS, event);
    }

    // == Presence Events ==
    
    public void sendOnlineStatusEvent(OnlineStatusEvent event) {
        UUID userId = event.getUserId();
        String userKey = (userId != null) ? userId.toString() : "unknown";

        log.info("=== KAFKA PRODUCER === Sending OnlineStatusEvent: user={}, online={}",
                userKey, event.isOnline());

        // Dùng userId làm partition key để đảm bảo thứ tự
        kafkaTemplate.send(T_ONLINE_STATUS, userKey, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("OnlineStatusEvent sent successfully for user: {}", userKey);
                    } else {
                        log.error("Failed to send OnlineStatusEvent for user {}: {}", userKey, ex.getMessage());
                    }
                });
    }

    // == Message Events ==

    public void sendMessageEvent(Object event) {
        kafkaTemplate.send(T_MESSAGE, event);
    }

    public void sendReactionEvent(MessageReactionEvent event) {
        kafkaTemplate.send(T_MESSAGE_REACTION, event);
    }

    public void sendReadReceiptEvent(Object event) {
        kafkaTemplate.send(T_MESSAGE_READ, event);
    }

    public void sendPinEvent(Object event) {
        kafkaTemplate.send(T_MESSAGE_PIN, event);
    }

    public void sendAttachmentEvent(Object event) {
        kafkaTemplate.send(T_MESSAGE_ATTACHMENT, event);
    }

    // == Notification Events ==
    
    public void sendNotificationEvent(Object event) {
        kafkaTemplate.send(T_NOTIFICATION, event);
    }
}