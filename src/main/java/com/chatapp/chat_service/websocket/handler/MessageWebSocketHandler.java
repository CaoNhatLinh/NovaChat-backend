package com.chatapp.chat_service.websocket.handler;

//import com.chatapp.chat_service.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {
//    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public MessageWebSocketHandler( ObjectMapper objectMapper) {
//        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Xử lý tin nhắn WebSocket ở đây
    }
}