//package com.chatapp.chat_service.mapper;
//
//import com.chatapp.chat_service.model.dto.NotificationDTO;
//import com.chatapp.chat_service.model.entity.Notification;
//import org.springframework.stereotype.Component;
//
//@Component
//public class NotificationMapper {
//
//    public static NotificationDTO toDTO(Notification notification) {
//        return NotificationDTO.builder()
//                .notificationId(notification.getKey().getNotificationId())
//                .userId(notification.getKey().getUserId())
//                .title(notification.getTitle())
//                .body(notification.getBody())
//                .type(notification.getType())
//                .metadata(notification.getMetadata())
//                .isRead(notification.isRead())
//                .createdAt(notification.getCreatedAt())
//                .build();
//    }
//
//    public Notification toEntity(NotificationDTO dto) {
//
//        return Notification.builder()
//                .key(new Notification.NotificationKey(dto.getUserId(), dto.getNotificationId()))
//                .title(dto.getTitle())
//                .body(dto.getBody())
//                .type(dto.getType())
//                .metadata(dto.getMetadata())
//                .isRead(dto.isRead())
//                .createdAt(dto.getCreatedAt())
//                .build();
//    }
//}