package com.chatapp.chat_service.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.chatapp.chat_service.auth.entity.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID user_id;
    private String username;
    private String display_name;
    private String nickname;
    private String avatar_url;
    private String created_at;
    public UserDTO(User user) {
        this.user_id = user.getUser_id();
        this.username = user.getUsername();
        this.display_name = user.getDisplay_name();
        this.nickname = user.getNickname();
        this.avatar_url = user.getAvatar_url();
        this.created_at = user.getCreated_at() != null ? user.getCreated_at().toString() : null;
    }
}
