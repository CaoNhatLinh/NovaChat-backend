package com.chatapp.chat_service.controller;

import com.chatapp.chat_service.model.dto.UserDTO;
import com.chatapp.chat_service.model.entity.User;
import com.chatapp.chat_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public ResponseEntity<UserDTO> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(userService.findByUsername(q));
    }
}