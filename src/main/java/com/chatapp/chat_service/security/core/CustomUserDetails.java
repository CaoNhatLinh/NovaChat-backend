package com.chatapp.chat_service.security.core;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.chatapp.chat_service.auth.entity.User;

import java.util.Collection;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public UUID getUserId() {
        return user.getUser_id() != null ? user.getUser_id() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // hoặc List.of(new SimpleGrantedAuthority("ROLE_USER")) nếu có roles
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
