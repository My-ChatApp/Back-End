package iuh.fit.userservice.event.consumer;

import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.entity.friend.FriendRequest;
import iuh.fit.userservice.entity.friend.FriendRequestStatus;
import iuh.fit.userservice.event.payload.UserPresenceChangedEvent;
import iuh.fit.userservice.event.payload.UserPresenceNotifyEvent;
import iuh.fit.userservice.repository.FriendRequestRepository;
import iuh.fit.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
public class PresenceConsumer {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.presence.exchange:presence.exchange}")
    private String presenceExchange;

    @Value("${rabbitmq.presence.routing-key.notify:user.presence.notify}")
    private String notifyRoutingKey;

    @RabbitListener(queues = "${rabbitmq.presence.queue.changed:user.presence.changed.queue}")
    @Transactional
    public void handlePresenceChanged(UserPresenceChangedEvent event) {
        validate(event);
        UUID userId = UUID.fromString(event.getUserId());
        log.info("Cập nhật presence userId={} online={}", userId, event.isOnline());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setOnline(event.isOnline());
        user.setLastSeenAt(event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now());
        userRepository.save(user);

        fanOutToFriends(user, event);
    }

    private void fanOutToFriends(User user, UserPresenceChangedEvent event) {
        List<FriendRequest> friendships = friendRequestRepository.findFriendsByUserId(
                user.getId(), FriendRequestStatus.ACCEPTED);

        Instant occurredAt = event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now();

        for (FriendRequest friendship : friendships) {
            UUID friendId = friendship.getSenderId().equals(user.getId())
                    ? friendship.getReceiverId()
                    : friendship.getSenderId();

            UserPresenceNotifyEvent notifyEvent = UserPresenceNotifyEvent.builder()
                    .userId(user.getId().toString())
                    .targetUserId(friendId.toString())
                    .displayName(user.getDisplayName())
                    .online(event.isOnline())
                    .occurredAt(occurredAt)
                    .build();

            rabbitTemplate.convertAndSend(presenceExchange, notifyRoutingKey, notifyEvent);
        }

        log.debug("Fan-out presence userId={} tới {} bạn bè", user.getId(), friendships.size());
    }

    private static void validate(UserPresenceChangedEvent event) {
        if (event == null || event.getUserId() == null) {
            throw new IllegalArgumentException("UserPresenceChangedEvent thiếu userId");
        }
    }
}
