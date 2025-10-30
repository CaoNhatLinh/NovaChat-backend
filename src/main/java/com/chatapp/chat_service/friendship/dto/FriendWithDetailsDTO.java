package com.chatapp.chat_service.friendship.dto;

import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.auth.entity.User;
import com.chatapp.chat_service.friendship.entity.Friendship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendWithDetailsDTO {

    private UUID userId;
    private Friendship.Status status;
    private UserDTO userDetails;
    public FriendWithDetailsDTO(Friendship friendship, User user) {
        this.userId = friendship.getKey().getUserId();
        this.status = friendship.getStatus();
        this.userDetails = new UserDTO(user);
    }

}