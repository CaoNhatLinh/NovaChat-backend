package com.chatapp.chat_service.kafka;


import com.chatapp.chat_service.websocket.event.FriendshipStatusEvent;
import com.chatapp.chat_service.service.MaterializedViewService;
import com.chatapp.chat_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    
    private final MaterializedViewService materializedViewService;
    private final NotificationService notificationService;
    
    public KafkaConsumerService(MaterializedViewService materializedViewService,
                                NotificationService notificationService) {
        this.materializedViewService = materializedViewService;
        this.notificationService = notificationService;
    }
    
    @KafkaListener(
            topics = "friendship-status-events",
            containerFactory = "friendshipStatusListenerFactory"
    )
    public void handleFriendshipStatusUpdate(
            @Payload FriendshipStatusEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("Received friendship status event from topic: {}, partition: {}, offset: {}", 
                   topic, partition, offset);
        
        try {
            // Validate the event
            if (event == null) {
                logger.warn("Received null friendship status event");
                return;
            }
            
            if (event.getSenderId() == null || event.getReceiverId() == null) {
                logger.warn("Received friendship status event with null sender or receiver ID: {}", event);
                return;
            }
            
            logger.info("Processing friendship status update: sender={}, receiver={}, status={}", 
                       event.getSenderId(), event.getReceiverId(), event.getStatus());
            
            // Process the friendship view update
            materializedViewService.updateFriendshipView(
                    event.getSenderId(),
                    event.getReceiverId(),
                    event.getStatus()
            );
            
            // Send notification
            notificationService.sendFriendshipUpdateNotification(
                    event.getReceiverId(),
                    event.getSenderId(),
                    event.getStatus()
            );
            
            logger.info("Successfully processed friendship status event for sender={}, receiver={}", 
                       event.getSenderId(), event.getReceiverId());
            
            // Manually acknowledge the message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            logger.error("Error processing friendship status event: {}", e.getMessage(), e);
            // Don't acknowledge on error - let the error handler deal with retries
            throw e;
        }
    }
}