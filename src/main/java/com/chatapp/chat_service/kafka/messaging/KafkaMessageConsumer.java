package com.chatapp.chat_service.kafka.messaging;



import com.chatapp.chat_service.model.dto.MessageResponseDto;
import com.chatapp.chat_service.service.MessageService;
import com.chatapp.chat_service.service.presence.OnlineStatusService;
import com.chatapp.chat_service.service.presence.PresenceService;
import com.chatapp.chat_service.websocket.event.MessageEvent;
import com.chatapp.chat_service.websocket.event.OnlineStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final OnlineStatusService onlineStatusService;
//    private static PresenceService presenceService;
    @KafkaListener(topics = "message-topic", containerFactory = "messageEventListenerFactory")
    public void handleMessageEvent(MessageEvent event) {

        try {
            // 1. LƯU TIN NHẮN VÀO DATABASE (nếu có MessageRequest)
            if (event.getMessageRequest() != null) {
                log.info("Processing MessageRequest for database save...");
                MessageResponseDto savedMessageDto = messageService.sendMessage(event.getMessageRequest());
                log.info("=== Message saved to database successfully ===");
                log.info("Saved message ID: {}", savedMessageDto.getMessageId());
                log.info("Saved message with sender: {}", savedMessageDto.getSender());
                
                // 2. GỬI TIN NHẮN ĐÃ LƯU ĐẾN CÁC CLIENT QUA WEBSOCKET  
                messagingTemplate.convertAndSend(
                        "/topic/conversation/" + event.getConversationId(),
                        savedMessageDto // Gửi MessageResponseDto với sender object đầy đủ
                );
                log.info("Message broadcasted to WebSocket clients");
            } else {
                // 3. FALLBACK: Gửi event trực tiếp nếu không có MessageRequest
                log.warn("MessageEvent without MessageRequest, broadcasting raw event");
                messagingTemplate.convertAndSend(
                        "/topic/conversation/" + event.getConversationId(),
                        event
                );
            }
        } catch (Exception e) {
            log.error("=== ERROR processing MessageEvent ===");
            log.error("Error details: {}", e.getMessage(), e);
            // TODO: Implement dead letter queue or retry mechanism
        }
    }

    @KafkaListener(topics = "online-status-topic", containerFactory = "onlineStatusEventListenerFactory")
    public void handleOnlineStatusEvent(OnlineStatusEvent event, 
                                        Acknowledgment acknowledgment) {
        log.debug("=== PROCESSING OnlineStatusEvent ===");
        log.debug("User: {}, Online: {}, Timestamp: {}", 
                 event.getUserId(), event.isOnline(), event.getTimestamp());

        try {

            // CRITICAL: Skip events older than 30 minutes to prevent processing stale events
            if (event.getTimestamp() != null) {
                Duration age = Duration.between(event.getTimestamp(), Instant.now());
                if (age.toMinutes() > 30) {
                    log.warn("DROPPING stale OnlineStatusEvent - Age: {} minutes, User: {}", 
                             age.toMinutes(), event.getUserId());
                    acknowledgment.acknowledge();
                    return;
                }
                
                // Also skip future events (clock skew protection)
                if (age.toMinutes() < -5) {
                    log.warn("DROPPING future OnlineStatusEvent - Future by: {} minutes, User: {}", 
                             Math.abs(age.toMinutes()), event.getUserId());
                    acknowledgment.acknowledge();
                    return;
                }
            }

            // Get current status before processing
            Set<String> onlineUsersBefore = onlineStatusService.getOnlineUsers();
            String userIdStr = event.getUserId().toString();
            boolean wasOnlineBefore = onlineUsersBefore.contains(userIdStr);
            
            log.debug("User {} was online before: {}", userIdStr, wasOnlineBefore);

            // Only process if status actually changes
            if (event.isOnline() && wasOnlineBefore) {
                log.debug("User {} already online - skipping duplicate online event", userIdStr);
                acknowledgment.acknowledge();
                return;
            }
            
            if (!event.isOnline() && !wasOnlineBefore) {
                log.debug("User {} already offline - skipping duplicate offline event", userIdStr);
                acknowledgment.acknowledge();
                return;
            }

            // Update status in Redis
            onlineStatusService.setUserOnlineStatus(event.getUserId(), event.isOnline());

            // Broadcast to clients only if status changed
            messagingTemplate.convertAndSend("/topic/online-status", event);

            // Log final status
            Set<String> onlineUsersAfter = onlineStatusService.getOnlineUsers();
            log.info("Status CHANGED - User: {}, Online: {}, Total online: {}", 
                     userIdStr, event.isOnline(), onlineUsersAfter.size());
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("ERROR processing OnlineStatusEvent: {}", e.getMessage(), e);
            // Don't acknowledge on error - message will be retried
            throw e; // Let error handler manage retries
        }
    }
}