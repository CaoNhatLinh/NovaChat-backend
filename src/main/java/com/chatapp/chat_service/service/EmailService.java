package com.chatapp.chat_service.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {
    public void sendFriendRequestUpdateEmail(String toEmail, String fromUser, String status) {
        // In a real application, this would send an actual email
        System.out.printf("Sending email to %s: Your friend request from %s has been %s%n",
                toEmail, fromUser, status.toLowerCase());
    }
}
