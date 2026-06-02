package iuh.fit.notificationservice.event.consumer;

import iuh.fit.notificationservice.config.RabbitMQConfig;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.NotificationType;
import iuh.fit.notificationservice.event.payload.FriendAcceptedEvent;
import iuh.fit.notificationservice.event.payload.FriendRequestSentEvent;
import iuh.fit.notificationservice.event.payload.LoginLockoutEvent;
import iuh.fit.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;

    /**
     * Routing key {@code friend.request.sent} — chỉ khi user-service gọi {@code sendRequest}.
     */
    @RabbitListener(queues = RabbitMQConfig.FRIEND_REQUEST_QUEUE)
    @Transactional
    public void handleFriendRequest(FriendRequestSentEvent event) {
        validateFriendRequestEvent(event);
        log.info("Nhận FRIEND_REQUEST: {} → {}", event.getSenderId(), event.getReceiverId());

        UUID receiverId = UUID.fromString(event.getReceiverId());
        UUID referenceId = UUID.fromString(event.getRequestId());

        if (alreadyExists(receiverId, NotificationType.FRIEND_REQUEST, referenceId)) {
            log.info("Bỏ qua FRIEND_REQUEST trùng referenceId={}", referenceId);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("senderId", event.getSenderId());
        data.put("senderName", event.getSenderName() != null ? event.getSenderName() : "");

        Notification notification = Notification.builder()
                .userId(receiverId)
                .actorId(UUID.fromString(event.getSenderId()))
                .type(NotificationType.FRIEND_REQUEST)
                .title("Lời mời kết bạn")
                .body((event.getSenderName() != null ? event.getSenderName() : "Ai đó") + " muốn kết bạn với bạn")
                .referenceId(referenceId)
                .data(data)
                .read(false)
                .expireAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Routing key {@code friend.request.accepted} — chỉ khi {@code acceptRequest}.
     */
    @RabbitListener(queues = RabbitMQConfig.FRIEND_ACCEPTED_QUEUE)
    @Transactional
    public void handleFriendAccepted(FriendAcceptedEvent event) {
        validateFriendAcceptedEvent(event);
        log.info("Nhận FRIEND_ACCEPTED: {} chấp nhận {}", event.getReceiverId(), event.getSenderId());

        UUID senderId = UUID.fromString(event.getSenderId());
        UUID referenceId = UUID.fromString(event.getRequestId());

        if (alreadyExists(senderId, NotificationType.FRIEND_ACCEPTED, referenceId)) {
            log.info("Bỏ qua FRIEND_ACCEPTED trùng referenceId={}", referenceId);
            return;
        }

        String receiverName = event.getReceiverName() != null ? event.getReceiverName() : "Người dùng";

        Map<String, Object> data = new HashMap<>();
        data.put("receiverId", event.getReceiverId());
        data.put("receiverName", receiverName);

        Notification notification = Notification.builder()
                .userId(senderId)
                .actorId(UUID.fromString(event.getReceiverId()))
                .type(NotificationType.FRIEND_ACCEPTED)
                .title("Chấp nhận kết bạn")
                .body(receiverName + " đã chấp nhận lời mời kết bạn")
                .referenceId(referenceId)
                .data(data)
                .read(false)
                .expireAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        notificationRepository.save(notification);
    }

    @RabbitListener(queues = "${rabbitmq.login-lockout.queue:auth.login-lockout.queue}")
    @Transactional
    public void handleLoginLockout(LoginLockoutEvent event) {
        validateLoginLockoutEvent(event);
        log.info("[NotificationConsumer] Login lockout alert for userId={}", event.getUserId());

        UUID userId = UUID.fromString(event.getUserId());
        UUID referenceId = UUID.nameUUIDFromBytes(
                ("login-lockout:" + event.getUserId() + ":" + event.getLockedUntil()).getBytes()
        );

        if (alreadyExists(userId, NotificationType.SYSTEM, referenceId)) {
            log.info("[NotificationConsumer] Skip duplicate SYSTEM login-lockout referenceId={}", referenceId);
            return;
        }

        String ip = event.getClientIp() != null ? event.getClientIp() : "không xác định";
        int minutes = event.getLockoutMinutes() > 0 ? event.getLockoutMinutes() : 5;

        Map<String, Object> data = new HashMap<>();
        data.put("reason", "LOGIN_LOCKOUT");
        data.put("clientIp", ip);
        data.put("email", event.getEmail());
        if (event.getLockedUntil() != null) {
            data.put("lockedUntil", event.getLockedUntil().toString());
        }
        data.put("lockoutMinutes", minutes);

        Notification notification = Notification.builder()
                .userId(userId)
                .type(NotificationType.SYSTEM)
                .title("Cảnh báo đăng nhập")
                .body("Có nhiều lần đăng nhập thất bại từ IP "
                        + ip
                        + ". Đăng nhập từ IP này bị tạm khóa "
                        + minutes
                        + " phút. Nếu không phải bạn, hãy đổi mật khẩu.")
                .referenceId(referenceId)
                .data(data)
                .read(false)
                .expireAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        notificationRepository.save(notification);
    }

    private boolean alreadyExists(UUID userId, NotificationType type, UUID referenceId) {
        return notificationRepository.existsByUserIdAndTypeAndReferenceIdAndDeletedFalse(
                userId, type, referenceId);
    }

    private static void validateFriendRequestEvent(FriendRequestSentEvent event) {
        if (event == null
                || event.getRequestId() == null
                || event.getSenderId() == null
                || event.getReceiverId() == null) {
            throw new IllegalArgumentException("FriendRequestSentEvent thiếu trường bắt buộc");
        }
    }

    private static void validateFriendAcceptedEvent(FriendAcceptedEvent event) {
        if (event == null
                || event.getRequestId() == null
                || event.getSenderId() == null
                || event.getReceiverId() == null) {
            throw new IllegalArgumentException("FriendAcceptedEvent thiếu trường bắt buộc");
        }
    }

    private static void validateLoginLockoutEvent(LoginLockoutEvent event) {
        if (event == null || event.getUserId() == null || event.getUserId().isBlank()) {
            throw new IllegalArgumentException("LoginLockoutEvent thiếu userId");
        }
    }
}
