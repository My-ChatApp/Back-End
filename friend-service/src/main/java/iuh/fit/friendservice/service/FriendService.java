package iuh.fit.friendservice.service;

import iuh.fit.friendservice.entity.FriendRequest;
import iuh.fit.friendservice.entity.enums.FriendRequestStatus;
import iuh.fit.friendservice.event.payload.FriendAcceptedEvent;
import iuh.fit.friendservice.event.payload.FriendRequestSentEvent;
import iuh.fit.friendservice.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.friend-request}")
    private String friendRequestKey;

    @Value("${rabbitmq.routing-key.friend-accepted}")
    private String friendAcceptedKey;

    // A gửi lời mời cho B
    public FriendRequest sendRequest(String senderId, String receiverId) {

        // Kiểm tra đã gửi chưa
        boolean exists = friendRequestRepository
                .existsBySenderIdAndReceiverIdAndStatus(
                    senderId, receiverId, FriendRequestStatus.PENDING
                );
        if (exists) {
            throw new RuntimeException("Đã gửi lời mời rồi");
        }

        FriendRequest request = FriendRequest.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .status(FriendRequestStatus.PENDING)
                .build();

        FriendRequest saved = friendRequestRepository.save(request);

        // Publish event
        rabbitTemplate.convertAndSend(exchange, friendRequestKey,
            FriendRequestSentEvent.builder()
                .requestId(saved.getId())
                .senderId(senderId)
                .receiverId(receiverId)
                .sentAt(saved.getCreatedAt())
                .build()
        );

        return saved;
    }

    // B chấp nhận lời mời
    public FriendRequest acceptRequest(String requestId, String receiverId) {

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getReceiverId().equals(receiverId)) {
            throw new RuntimeException("Không có quyền");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        FriendRequest saved = friendRequestRepository.save(request);

        // Publish event → Chat-Service tạo conversation
        //                → Noti-Service lưu thông báo
        rabbitTemplate.convertAndSend(exchange, friendAcceptedKey,
            FriendAcceptedEvent.builder()
                .requestId(saved.getId())
                .senderId(request.getSenderId())
                .receiverId(receiverId)
                .acceptedAt(saved.getUpdatedAt())
                .build()
        );

        return saved;
    }
}