package com.chatapp.chat_service.friendship.service;

import com.chatapp.chat_service.auth.dto.UserDTO;
import com.chatapp.chat_service.auth.entity.User;
import com.chatapp.chat_service.auth.repository.UserRepository;
import com.chatapp.chat_service.common.exception.BusinessException;
import com.chatapp.chat_service.common.exception.ConflictException;
import com.chatapp.chat_service.common.exception.NotFoundException;
import com.chatapp.chat_service.friendship.dto.FriendRequestNotification;
import com.chatapp.chat_service.friendship.dto.FriendRequestUpdate;
import com.chatapp.chat_service.friendship.dto.FriendRequestsResponse;
import com.chatapp.chat_service.friendship.dto.FriendWithDetailsDTO;
import com.chatapp.chat_service.friendship.entity.Friendship;
import com.chatapp.chat_service.friendship.event.FriendshipStatusEvent;
import com.chatapp.chat_service.friendship.repository.FriendshipRepository;
import com.chatapp.chat_service.kafka.KafkaEventProducer;
import com.chatapp.chat_service.security.core.SecurityContextHelper;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class FriendService {
//    private final FriendshipRepository friendshipRepository;
//    private final FriendRequestRepository friendRequestRepository;
//    private final UserRepository userRepository;
//    private final NotificationService notificationService;
//    private final JwtService jwtService;
//    private final CassandraOperations cassandraOperations;
    private final FriendshipRepository friendshipRepository;
    private final KafkaEventProducer KafkaEventProducer;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;



    public FriendRequestsResponse getFriendDetailsByStatus(UUID userId, String status) {
        UUID currentUserId = securityContextHelper.getCurrentUserId();

        List<Friendship> friendships = friendshipRepository.findByUserOrFriendAndStatus(userId, status);

        List<UserDTO> userDetails = friendships.stream()
                .map(f -> f.getKey().getUserId().equals(userId)
                        ? f.getKey().getFriendId()
                        : f.getKey().getUserId())
                .distinct()
                .filter(friendId -> !friendId.equals(currentUserId))
                .map(friendId -> userRepository.findById(friendId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(UserDTO::new)
                .collect(Collectors.toList());

        return new FriendRequestsResponse(
                userId,
                friendships.isEmpty() ? null : friendships.get(0).getStatus().toString(),
                userDetails
        );
    }
    @Transactional
    public void sendFriendRequest(UUID senderId, UUID receiverId) {

        UUID userId = securityContextHelper.getCurrentUserId();

        if( senderId == null || receiverId == null) {
            throw new BusinessException("Sender and receiver IDs cannot be null");
        }
        if (!senderId.equals(userId)) {
            throw new ConflictException("Sender ID does not match the authenticated user");
        }


        if (friendshipRepository.findByUserAndFriend(senderId, receiverId).isPresent()) {
            throw new BusinessException("Friend request already exists");
        }

        if( senderId.equals(receiverId)) {
            throw new ConflictException("Cannot send friend request to yourself");
        }
        Friendship friendship = new Friendship();
        friendship.setKey(new Friendship.FriendshipKey(senderId, receiverId));
        friendship.setStatus(Friendship.Status.PENDING);
        friendship.setCreatedAt(Instant.now());

        friendshipRepository.save(friendship);

        // Send Kafka event
        KafkaEventProducer.sendFriendRequestEvent(senderId, receiverId);

        // Send WebSocket notification
        FriendRequestNotification notification = new FriendRequestNotification(senderId, receiverId);
        User sender = userRepository.findById(senderId).orElse(null);
        if (sender != null) {
            notification.setMessage(String.format("%s sent you a friend request", sender.getDisplay_name()));
        }

    }

    @Transactional
    public FriendRequestsResponse getReceivedFriendRequestsWithDetails(UUID userId) {
        // 1. Lấy danh sách friendship
        List<Friendship> friendships = friendshipRepository.findFriendRecevidRequest(userId);

        // 2. Lấy danh sách friendIds
        List<UUID> friendIds = friendships.stream()
                .map(f -> f.getKey().getUserId())
                .collect(Collectors.toList());

        // 3. Lấy thông tin users
        List<UserDTO> userDetails = userRepository.findUsersByIds(friendIds)
                .stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());

        // 4. Tạo response
        FriendRequestsResponse response = new FriendRequestsResponse(
                userId,
                friendships.isEmpty() ? null : String.valueOf(friendships.get(0).getStatus()),
                userDetails
        );
        return response;
    }


    @Transactional
    public void acceptFriendRequest(UUID receiverId, UUID senderId) {
        // 0. Kiểm tra người dùng hiện tại
        if (receiverId == null || senderId == null) {
            throw new BusinessException("Receiver and sender IDs cannot be null");
        }
        // 1. Kiểm tra tồn tại friendship chính
        Friendship friendship = friendshipRepository.findByUserAndFriend(senderId, receiverId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        // 2. Kiểm tra friendship nghịch đảo
        Friendship inverseFriendship = friendshipRepository
                .findByUserAndFriend(receiverId, senderId)
                .orElseGet(() -> {
                    Friendship newInverse = new Friendship();
                    newInverse.setKey(new Friendship.FriendshipKey(receiverId, senderId));
                    newInverse.setStatus(Friendship.Status.ACCEPTED);
                    newInverse.setCreatedAt(Instant.now());
                    return newInverse;
                });

        // 3. Cập nhật trạng thái
        friendship.setStatus(Friendship.Status.ACCEPTED);
        friendship.setUpdatedAt(Instant.now());

        inverseFriendship.setStatus(Friendship.Status.ACCEPTED);
        inverseFriendship.setUpdatedAt(Instant.now());

        friendshipRepository.saveAll(List.of(friendship, inverseFriendship));

        // 4. Gửi event (sử dụng enum)
        KafkaEventProducer.sendFriendshipStatusEvent(
                new FriendshipStatusEvent(
                        senderId,
                        receiverId,
                        Friendship.Status.ACCEPTED // Sử dụng enum
                )
        );
    }
    @Transactional
    public void rejectFriendRequest(UUID receiverId, UUID senderId) {
        Friendship friendship = friendshipRepository.findByUserAndFriend(senderId, receiverId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));

        friendship.setStatus(Friendship.Status.REJECTED);
        friendship.setUpdatedAt(Instant.now());
        friendshipRepository.save(friendship);

        // Send Kafka event
        KafkaEventProducer.sendFriendshipStatusEvent(
                new FriendshipStatusEvent(senderId, receiverId, Friendship.Status.REJECTED));

        // Send WebSocket notification
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/friend-requests",
                new FriendRequestUpdate(receiverId, "REJECTED")
        );
    }
    public List<FriendWithDetailsDTO> getFriendsWithDetails(UUID userId) {
        // 1. Lấy danh sách friendship
        List<Friendship> friendships = friendshipRepository.findByUserId(userId);

        // 2. Thu thập tất cả friend_id
        List<UUID> friendIds = friendships.stream()
                .map(Friendship -> Friendship.getKey().getFriendId())
                .collect(Collectors.toList());

        // 3. Lấy thông tin user với 1 query batch
        Map<UUID, User> userMap = userRepository.findUsersByIds(friendIds)
                .stream()
                .collect(Collectors.toMap(User::getUser_id, Function.identity()));

        // 4. Kết hợp dữ liệu
        return friendships.stream()
                .map(friendship -> {
                    User friend = userMap.get(friendship.getKey().getFriendId());

                    return new FriendWithDetailsDTO(
                            friendship,
                            friend
                    );
                })
                .collect(Collectors.toList());
    }
    public FriendRequestsResponse getFriendRequestsWithDetails(UUID userId) {
        // 1. Lấy danh sách friendship
        List<Friendship> friendships = friendshipRepository.findFriendRequest(userId);

        // 2. Lấy danh sách friendIds
        List<UUID> friendIds = friendships.stream()
                .map(f -> f.getKey().getFriendId())
                .collect(Collectors.toList());

        // 3. Lấy thông tin users
        List<UserDTO> userDetails = userRepository.findUsersByIds(friendIds)
                .stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());

        // 4. Tạo response
        return new FriendRequestsResponse(
                userId,
                friendships.isEmpty() ? null : String.valueOf(friendships.get(0).getStatus()),
                userDetails
        );
    }

//    private void createFriendship(UUID userId, UUID friendId) {
//        Friendship friendship = new Friendship();
//        FriendshipKey key = new FriendshipKey();
//        key.setUserId(userId);
//        key.setFriendId(friendId);
//        friendship.setKey(key);
//        friendship.setCreatedAt(Instant.now());
//        friendship.setStatus("ACCEPTED");
//
//        friendshipRepository.save(friendship);
//    }
//
//    private void sendFriendRequestNotification(UUID senderId, UUID receiverId) {
//        User sender = userRepository.findById(senderId)
//                .orElseThrow(() -> new NotFoundException("User not found"));
//
//        NotificationDTO notification = new NotificationDTO();
//        notification.setUserId(receiverId);
//        notification.setTitle("New Friend Request");
//        notification.setBody(String.format("%s sent you a friend request", sender.getUsername()));
//        notification.setType("FRIEND_REQUEST");
//
//        notificationService.createNotification(notification);
//    }
//
//
//
//    private void sendFriendRequestAcceptedNotification(UUID senderId, UUID receiverId) {
//        User receiver = userRepository.findById(receiverId)
//                .orElseThrow(() -> new NotFoundException("User not found"));
//
//        NotificationDTO notification = new NotificationDTO();
//        notification.setUserId(senderId);
//        notification.setTitle("Friend Request Accepted");
//        notification.setBody(String.format("%s accepted your friend request", receiver.getUsername()));
//        notification.setType("FRIEND_ACCEPTED");
//
//        notificationService.createNotification(notification);
//    }

}