package com.chatapp.chat_service.kafka;

import com.chatapp.chat_service.friendship.event.FriendshipStatusEvent;
import com.chatapp.chat_service.friendship.service.MaterializedViewService;
import com.chatapp.chat_service.message.dto.MessageResponseDto;
import com.chatapp.chat_service.message.event.MessageEvent;
import com.chatapp.chat_service.message.service.MessageService;
import com.chatapp.chat_service.notification.service.NotificationService;
import com.chatapp.chat_service.presence.event.OnlineStatusEvent;
import com.chatapp.chat_service.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventConsumer {

    // Services
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final PresenceService presenceService;
    private final MaterializedViewService materializedViewService;
    private final NotificationService notificationService;

    // == Message Listener (ĐÃ SỬA BUG MẤT TIN NHẮN) ==

    @KafkaListener(topics = "message-topic", containerFactory = "messageEventListenerFactory")
    public void handleMessageEvent(Object eventObject, Acknowledgment acknowledgment) {
        
        if (!(eventObject instanceof MessageEvent)) {
            log.warn("Received unknown message type: {}", eventObject.getClass().getName());
            acknowledgment.acknowledge();
            return;
        }

        MessageEvent event = (MessageEvent) eventObject;

        try {
            if (event.getMessageRequest() != null) {
                log.info("Processing MessageRequest for database save...");
                MessageResponseDto savedMessageDto = messageService.sendMessage(event.getMessageRequest());
                log.info("Message saved to database. ID: {}", savedMessageDto.getMessageId());

                messagingTemplate.convertAndSend(
                        "/topic/conversation/" + event.getConversationId(),
                        savedMessageDto
                );
                log.info("Message broadcasted to WebSocket clients");
            } else {
                log.warn("MessageEvent without MessageRequest, broadcasting raw event");
                messagingTemplate.convertAndSend(
                        "/topic/conversation/" + event.getConversationId(),
                        event
                );
            }
            
            // Chỉ ack() khi TẤT CẢ đã thành công
            acknowledgment.acknowledge(); 
            
        } catch (Exception e) {
            log.error("=== ERROR processing MessageEvent, will retry. Error: {}", e.getMessage(), e);
            // Ném lỗi để ErrorHandler có thể retry
            throw new RuntimeException("Failed to process message event, triggering retry", e); 
        }
    }

    // == Online Status Listener (Code gốc đã tốt, chỉ cần cast) ==

    @KafkaListener(topics = "online-status-topic", containerFactory = "onlineStatusEventListenerFactory")
    public void handleOnlineStatusEvent(Object eventObject, Acknowledgment acknowledgment) {
        
        if (!(eventObject instanceof OnlineStatusEvent)) {
            log.warn("Received unknown online status type: {}", eventObject.getClass().getName());
            acknowledgment.acknowledge();
            return;
        }
        
        OnlineStatusEvent event = (OnlineStatusEvent) eventObject;
        log.debug("Processing OnlineStatusEvent for User: {}", event.getUserId());

        try {
            // Logic xử lý stale/duplicate events của bạn (RẤT TỐT, giữ nguyên)
            if (event.getTimestamp() != null) {
                Duration age = Duration.between(event.getTimestamp(), Instant.now());
                if (age.toMinutes() > 30) {
                    log.warn("DROPPING stale OnlineStatusEvent. User: {}", event.getUserId());
                    acknowledgment.acknowledge();
                    return;
                }
                if (age.toMinutes() < -5) {
                    log.warn("DROPPING future OnlineStatusEvent. User: {}", event.getUserId());
                    acknowledgment.acknowledge();
                    return;
                }
            }

            Set<String> onlineUsersBefore = presenceService.getOnlineUsers();
            String userIdStr = event.getUserId().toString();
            boolean wasOnlineBefore = onlineUsersBefore.contains(userIdStr);

            if ((event.isOnline() && wasOnlineBefore) || (!event.isOnline() && !wasOnlineBefore)) {
                log.debug("User {} already in desired state - skipping duplicate event", userIdStr);
                acknowledgment.acknowledge();
                return;
            }

            // Logic chính
            presenceService.setUserOnlineStatus(event.getUserId(), event.isOnline());
            messagingTemplate.convertAndSend("/topic/online-status", event);
            
            log.info("Status CHANGED - User: {}, Online: {}", userIdStr, event.isOnline());
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("ERROR processing OnlineStatusEvent, will retry: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process online status event, triggering retry", e);
        }
    }

    // == Friendship Status Listener (Code gốc đã tốt, chỉ cần cast) ==
    
    @KafkaListener(topics = "friendship-status-events", containerFactory = "friendshipStatusListenerFactory")
    public void handleFriendshipStatusUpdate(Object eventObject, Acknowledgment acknowledgment) {
        
        if (!(eventObject instanceof FriendshipStatusEvent)) {
            log.warn("Received unknown friendship status type: {}", eventObject.getClass().getName());
            acknowledgment.acknowledge();
            return;
        }
        
        FriendshipStatusEvent event = (FriendshipStatusEvent) eventObject;
        log.info("Processing friendship status update: sender={}, receiver={}, status={}",
                   event.getSenderId(), event.getReceiverId(), event.getStatus());
        
        try {
            if (event.getSenderId() == null || event.getReceiverId() == null) {
                log.warn("Received event with null IDs, dropping: {}", event);
                acknowledgment.acknowledge();
                return;
            }
            
            // Logic chính
            materializedViewService.updateFriendshipView(
                    event.getSenderId(),
                    event.getReceiverId(),
                    event.getStatus()
            );
            
            notificationService.sendFriendshipUpdateNotification(
                    event.getReceiverId(),
                    event.getSenderId(),
                    event.getStatus()
            );
            
            log.info("Successfully processed friendship status event for sender={}, receiver={}",
                       event.getSenderId(), event.getReceiverId());
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing friendship status event, will retry: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process friendship status event, triggering retry", e);
        }
    }
    
    // TODO: Thêm các listener khác (cho reaction, read, pin...) tại đây
    // Ví dụ:
    /*
    @KafkaListener(topics = "message-reaction-topic", containerFactory = "messageEventListenerFactory")
    public void handleReactionEvent(Object eventObject, Acknowledgment acknowledgment) {
        // ... logic xử lý reaction ...
        acknowledgment.acknowledge();
    }
    */
}