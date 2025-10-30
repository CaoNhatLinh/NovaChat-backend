package com.chatapp.chat_service.friendship.controller;

import com.chatapp.chat_service.friendship.dto.FriendRequestResponse;
import com.chatapp.chat_service.friendship.dto.FriendRequestsResponse;
import com.chatapp.chat_service.friendship.dto.FriendWithDetailsDTO;
import com.chatapp.chat_service.friendship.entity.Friendship;
import com.chatapp.chat_service.friendship.service.FriendService;
import com.chatapp.chat_service.security.core.SecurityContextHelper;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendService friendshipService;
    private final SecurityContextHelper securityContextHelper;
    public FriendController(FriendService friendService,
                            SecurityContextHelper securityContextHelper) {
        this.friendshipService = friendService;
        this.securityContextHelper = securityContextHelper;
    }

    @GetMapping("/requests/sent/{userId}")
    public ResponseEntity<FriendRequestsResponse> getSentRequests(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(
                friendshipService.getFriendRequestsWithDetails(userId)
        );
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<FriendRequestsResponse> getFriendshipsByUserIdAndStatus(
            @PathVariable UUID userId,
            @PathVariable String status) {
        return ResponseEntity.ok(friendshipService.getFriendDetailsByStatus(userId, status));
    }
    @PostMapping("/request")
    public ResponseEntity<Void> sendFriendRequest(
            @RequestBody FriendRequestResponse request) {

        friendshipService.sendFriendRequest(
                request.getSenderId(),
                request.getReceiverId()
        );
        return ResponseEntity.accepted().build();
    }
    @PutMapping("/accept")
    public ResponseEntity<Void> acceptFriendRequest(
            @RequestBody FriendRequestResponse response) {
        friendshipService.acceptFriendRequest(
                response.getReceiverId(),
                response.getSenderId()
        );
        return ResponseEntity.ok().build();
    }
    @GetMapping("/")
    public ResponseEntity<FriendWithDetailsDTO> getFriendRequests() {
        UUID userId = securityContextHelper.getCurrentUserId();
        friendshipService.getFriendsWithDetails(userId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/requests/received/{userId}")
    public ResponseEntity<FriendRequestsResponse> getReceivedRequests(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(
                friendshipService.getReceivedFriendRequestsWithDetails(userId)
        );
    }
    @PutMapping("/reject")
    public ResponseEntity<Void> rejectFriendRequest(
            @RequestBody FriendRequestResponse response) {
        friendshipService.rejectFriendRequest(
                response.getReceiverId(),
                response.getSenderId()
        );
        return ResponseEntity.ok().build();
    }
//    @PostMapping("/accept/{requestId}")
//    public ResponseEntity<Void> acceptFriendRequest(
//            @RequestHeader("Authorization") String token,
//            @PathVariable UUID requestId
//    ) {
//        UUID senderId = securityContextHelper.getCurrentUserId();
//        friendService.acceptFriendRequest(requestId, senderId);
//        return ResponseEntity.ok().build();
//    }

//    @PostMapping("/block/{userIdToBlock}")
//    public ResponseEntity<Void> blockUser(
//
//            @PathVariable UUID userIdToBlock
//    ) {
//        UUID blockerId = securityContextHelper.getCurrentUserId();
//        friendService.blockUser(blockerId, userIdToBlock);
//        return ResponseEntity.ok().build();
//    }

//    @GetMapping("/status/{otherUserId}")
//    public ResponseEntity<FriendStatusResponse> getFriendStatus(
//            @PathVariable UUID otherUserId
//    ) {
//        UUID userId = securityContextHelper.getCurrentUserId();
//        boolean isFriend = friendService.areFriends(userId, otherUserId);
//        boolean isBlocked = friendService.isBlocked(userId, otherUserId);
//
//        return ResponseEntity.ok(new FriendStatusResponse(isFriend, isBlocked));
//    }
}