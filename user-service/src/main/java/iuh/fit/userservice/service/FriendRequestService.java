package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.request.CreateFriendRequestRequest;
import iuh.fit.userservice.dto.request.UpdateFriendRequestRequest;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.entity.friend.FriendRequest;
import iuh.fit.userservice.entity.friend.FriendRequestStatus;
import iuh.fit.userservice.event.payload.FriendAcceptedEvent;
import iuh.fit.userservice.event.payload.FriendRequestSentEvent;
import iuh.fit.userservice.exception.ResourceNotFoundException;
import iuh.fit.userservice.repository.FriendRequestRepository;
import iuh.fit.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.friend.exchange}")
    private String friendExchange;

    @Value("${rabbitmq.friend.routing-key.friend-request}")
    private String friendRequestKey;

    @Value("${rabbitmq.friend.routing-key.friend-accepted}")
    private String friendAcceptedKey;

    @Transactional(readOnly = true)
    public List<FriendRequest> findAll() {
        return friendRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public FriendRequest findById(UUID id) {
        return friendRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<FriendRequest> findIncoming(UUID userId) {
        return friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<FriendRequest> findOutgoing(UUID userId) {
        return friendRequestRepository.findBySenderIdAndStatus(userId, FriendRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<FriendRequest> findFriends(UUID userId) {
        return friendRequestRepository.findFriendsByUserId(userId, FriendRequestStatus.ACCEPTED);
    }

    public FriendRequest sendRequest(String senderId, String receiverId) {
        return sendRequest(new CreateFriendRequestRequest(senderId, receiverId, null));
    }

    public FriendRequest sendRequest(CreateFriendRequestRequest request) {
        UUID sender = UUID.fromString(request.getSenderId());
        UUID receiver = UUID.fromString(request.getReceiverId());

        if (sender.equals(receiver)) {
            throw new IllegalStateException("Không thể gửi lời mời cho chính mình");
        }

        boolean exists = friendRequestRepository
                .existsBySenderIdAndReceiverIdAndStatus(
                        sender, receiver, FriendRequestStatus.PENDING);
        if (exists) {
            throw new IllegalStateException("Đã gửi lời mời rồi");
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .senderId(sender)
                .receiverId(receiver)
                .message(request.getMessage())
                .status(FriendRequestStatus.PENDING)
                .build();

        FriendRequest saved = friendRequestRepository.save(friendRequest);
        publishFriendRequestSent(saved, request.getSenderId(), request.getReceiverId());
        return saved;
    }

    @Transactional
    public FriendRequest update(UUID id, UpdateFriendRequestRequest request) {
        FriendRequest friendRequest = findById(id);
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Chỉ sửa được lời mời PENDING");
        }
        if (request.getMessage() != null) {
            friendRequest.setMessage(request.getMessage());
        }
        return friendRequestRepository.save(friendRequest);
    }

    @Transactional
    public FriendRequest acceptRequest(String requestId, String receiverId) {
        return acceptRequest(UUID.fromString(requestId), UUID.fromString(receiverId));
    }

    @Transactional
    public FriendRequest acceptRequest(UUID requestId, UUID receiverId) {
        FriendRequest request = findById(requestId);

        if (!request.getReceiverId().equals(receiverId)) {
            throw new IllegalStateException("Không có quyền");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Lời mời không còn PENDING");
        }

        Instant now = Instant.now();
        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setRespondedAt(now);
        FriendRequest saved = friendRequestRepository.save(request);

        publishFriendAccepted(saved, receiverId);
        return saved;
    }

    @Transactional
    public FriendRequest rejectRequest(UUID requestId, UUID receiverId) {
        FriendRequest request = findById(requestId);

        if (!request.getReceiverId().equals(receiverId)) {
            throw new IllegalStateException("Không có quyền");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Lời mời không còn PENDING");
        }

        request.setStatus(FriendRequestStatus.REJECTED);
        request.setRespondedAt(Instant.now());
        return friendRequestRepository.save(request);
    }

    @Transactional
    public void cancelOutgoing(UUID requestId, UUID senderId) {
        FriendRequest request = findById(requestId);

        if (!request.getSenderId().equals(senderId)) {
            throw new IllegalStateException("Không có quyền");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalStateException("Lời mời không còn PENDING");
        }

        friendRequestRepository.delete(request);
    }

    @Transactional
    public void unfriend(UUID userId, UUID friendUserId) {
        if (userId.equals(friendUserId)) {
            throw new IllegalStateException("Không thể hủy kết bạn với chính mình");
        }

        FriendRequest friendship = friendRequestRepository
                .findAcceptedBetween(userId, friendUserId, FriendRequestStatus.ACCEPTED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy quan hệ bạn bè giữa " + userId + " và " + friendUserId));

        friendRequestRepository.delete(friendship);
    }

    private void publishFriendRequestSent(FriendRequest saved, String senderId, String receiverId) {
        String senderName = resolveDisplayName(saved.getSenderId());
        rabbitTemplate.convertAndSend(friendExchange, friendRequestKey,
                FriendRequestSentEvent.builder()
                        .requestId(saved.getId().toString())
                        .senderId(senderId)
                        .senderName(senderName)
                        .receiverId(receiverId)
                        .sentAt(saved.getCreatedAt())
                        .build()
        );
    }

    private void publishFriendAccepted(FriendRequest saved, UUID receiverId) {
        String receiverName = resolveDisplayName(receiverId);
        rabbitTemplate.convertAndSend(friendExchange, friendAcceptedKey,
                FriendAcceptedEvent.builder()
                        .requestId(saved.getId().toString())
                        .senderId(saved.getSenderId().toString())
                        .receiverId(receiverId.toString())
                        .receiverName(receiverName)
                        .acceptedAt(saved.getRespondedAt())
                        .build()
        );
    }

    private String resolveDisplayName(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getDisplayName)
                .orElse("Người dùng");
    }
}
