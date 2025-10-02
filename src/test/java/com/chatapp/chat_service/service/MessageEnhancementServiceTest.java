package com.chatapp.chat_service.service;

import com.chatapp.chat_service.kafka.messaging.KafkaMessageProducer;
import com.chatapp.chat_service.model.entity.Message;
import com.chatapp.chat_service.model.entity.MessageReaction;
import com.chatapp.chat_service.repository.MessageReactionRepository;
import com.chatapp.chat_service.repository.MessageRepository;
import com.chatapp.chat_service.websocket.event.MessageReactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageEnhancementServiceTest {

    @Mock
    private MessageReactionRepository reactionRepository;
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private NotificationService notificationService;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private KafkaMessageProducer kafkaProducer;

    @InjectMocks
    private MessageEnhancementService messageEnhancementService;

    private UUID conversationId;
    private UUID messageId;
    private UUID userId;
    private UUID messageOwnerId;
    private String emoji;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        userId = UUID.randomUUID();
        messageOwnerId = UUID.randomUUID();
        emoji = "❤️";
    }

    @Test
    void testToggleReaction_AddNewReaction_Success() {
        // Arrange
        MessageReaction.MessageReactionKey key = new MessageReaction.MessageReactionKey(conversationId, messageId, emoji, userId);
        when(reactionRepository.findById(key)).thenReturn(Optional.empty());
        
        Message message = new Message();
        message.setSenderId(messageOwnerId);
        when(messageRepository.findById(any())).thenReturn(Optional.of(message));

        // Act
        messageEnhancementService.toggleReaction(conversationId, messageId, emoji, userId);

        // Assert
        ArgumentCaptor<MessageReaction> reactionCaptor = ArgumentCaptor.forClass(MessageReaction.class);
        verify(reactionRepository).save(reactionCaptor.capture());
        
        MessageReaction savedReaction = reactionCaptor.getValue();
        assertEquals(key, savedReaction.getKey());
        assertNotNull(savedReaction.getReactedAt());

        // Verify WebSocket message sent
        ArgumentCaptor<MessageReactionEvent> eventCaptor = ArgumentCaptor.forClass(MessageReactionEvent.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/conversation/" + conversationId + "/reactions"), 
            eventCaptor.capture()
        );
        
        MessageReactionEvent event = eventCaptor.getValue();
        assertEquals("ADD", event.getAction());
        assertEquals(emoji, event.getEmoji());
        assertEquals(userId, event.getUserId());

        // Verify Kafka event sent
        verify(kafkaProducer).sendReactionEvent(any(MessageReactionEvent.class));

        // Verify notification created for message owner
        verify(notificationService).createReactionNotification(
            eq(messageOwnerId), eq(userId), anyString(), eq(emoji), eq(conversationId), eq(messageId)
        );
    }

    @Test
    void testToggleReaction_RemoveExistingReaction_Success() {
        // Arrange
        MessageReaction.MessageReactionKey key = new MessageReaction.MessageReactionKey(conversationId, messageId, emoji, userId);
        MessageReaction existingReaction = MessageReaction.builder()
            .key(key)
            .reactedAt(Instant.now())
            .build();
        
        when(reactionRepository.findById(key)).thenReturn(Optional.of(existingReaction));

        // Act
        messageEnhancementService.toggleReaction(conversationId, messageId, emoji, userId);

        // Assert
        verify(reactionRepository).delete(existingReaction);

        // Verify WebSocket message sent with REMOVE action
        ArgumentCaptor<MessageReactionEvent> eventCaptor = ArgumentCaptor.forClass(MessageReactionEvent.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/conversation/" + conversationId + "/reactions"), 
            eventCaptor.capture()
        );
        
        MessageReactionEvent event = eventCaptor.getValue();
        assertEquals("REMOVE", event.getAction());

        // Verify Kafka event sent
        verify(kafkaProducer).sendReactionEvent(any(MessageReactionEvent.class));

        // Verify NO notification created for removal
        verify(notificationService, never()).createReactionNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testToggleReaction_UserReactsToOwnMessage_NoNotification() {
        // Arrange
        MessageReaction.MessageReactionKey key = new MessageReaction.MessageReactionKey(conversationId, messageId, emoji, userId);
        when(reactionRepository.findById(key)).thenReturn(Optional.empty());
        
        Message message = new Message();
        message.setSenderId(userId); // Same user as reactor
        when(messageRepository.findById(any())).thenReturn(Optional.of(message));

        // Act
        messageEnhancementService.toggleReaction(conversationId, messageId, emoji, userId);

        // Assert
        verify(reactionRepository).save(any(MessageReaction.class));
        verify(messagingTemplate).convertAndSend(anyString(), any(MessageReactionEvent.class));
        verify(kafkaProducer).sendReactionEvent(any(MessageReactionEvent.class));

        // Verify NO notification created (user reacted to their own message)
        verify(notificationService, never()).createReactionNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testToggleReaction_MessageNotFound_NoNotification() {
        // Arrange
        MessageReaction.MessageReactionKey key = new MessageReaction.MessageReactionKey(conversationId, messageId, emoji, userId);
        when(reactionRepository.findById(key)).thenReturn(Optional.empty());
        when(messageRepository.findById(any())).thenReturn(Optional.empty()); // Message not found

        // Act
        messageEnhancementService.toggleReaction(conversationId, messageId, emoji, userId);

        // Assert
        verify(reactionRepository).save(any(MessageReaction.class));
        verify(messagingTemplate).convertAndSend(anyString(), any(MessageReactionEvent.class));
        verify(kafkaProducer).sendReactionEvent(any(MessageReactionEvent.class));

        // Verify NO notification created (message not found)
        verify(notificationService, never()).createReactionNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testToggleReaction_NotificationServiceFails_ContinuesExecution() {
        // Arrange
        MessageReaction.MessageReactionKey key = new MessageReaction.MessageReactionKey(conversationId, messageId, emoji, userId);
        when(reactionRepository.findById(key)).thenReturn(Optional.empty());
        
        Message message = new Message();
        message.setSenderId(messageOwnerId);
        when(messageRepository.findById(any())).thenReturn(Optional.of(message));
        
        doThrow(new RuntimeException("Notification service error"))
            .when(notificationService).createReactionNotification(any(), any(), any(), any(), any(), any());

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            messageEnhancementService.toggleReaction(conversationId, messageId, emoji, userId);
        });

        // Verify other operations still completed
        verify(reactionRepository).save(any(MessageReaction.class));
        verify(messagingTemplate).convertAndSend(anyString(), any(MessageReactionEvent.class));
        verify(kafkaProducer).sendReactionEvent(any(MessageReactionEvent.class));
    }
}
