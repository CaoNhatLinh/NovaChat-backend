package com.chatapp.chat_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger {

    @EventListener

    public void onReady(ApplicationReadyEvent event) {
        System.out.println("✅ ChatApp backend đã khởi động thành công!");
        System.out.println("📡 WebSocket sẵn sàng tại: ws://localhost:8084/ws");
        System.out.println("🔐 JWT Enabled | Redis | Cassandra | Kafka");
    }
}
