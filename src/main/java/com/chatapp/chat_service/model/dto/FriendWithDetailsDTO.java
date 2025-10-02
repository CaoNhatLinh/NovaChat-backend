package com.chatapp.chat_service.model.dto;

import com.chatapp.chat_service.model.entity.Friendship;
import com.chatapp.chat_service.model.entity.User;
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