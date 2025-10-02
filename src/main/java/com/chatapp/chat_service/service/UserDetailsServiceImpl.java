package com.chatapp.chat_service.service;

import com.chatapp.chat_service.model.entity.CustomUserDetails;
import com.chatapp.chat_service.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



import com.chatapp.chat_service.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Sửa lại tên phương thức cho đúng convention (findFirstByUsername -> findByUsername)
        Optional<User> userOptional = userRepository.findFirstByUsername(username);

        // Kiểm tra Optional có giá trị hay không
        User user = userOptional.orElseThrow(() ->
                new UsernameNotFoundException("User not found with username: " + username));

        return new CustomUserDetails(user);
    }

}