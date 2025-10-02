package com.chatapp.chat_service.websocket.event;

import com.chatapp.chat_service.model.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUserEvent {
    private UUID conversationId;
    private UserDTO user;
}
