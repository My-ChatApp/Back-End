package iuh.fit.notificationservice.event.consumer;

import iuh.fit.notificationservice.config.RabbitMQConfig;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.event.payload.FriendAcceptedEvent;
import iuh.fit.notificationservice.event.payload.FriendRequestSentEvent;
import iuh.fit.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;

    // Nhận khi A gửi lời mời cho B
    @RabbitListener(queues = RabbitMQConfig.FRIEND_REQUEST_QUEUE)
    public void handleFriendRequest(FriendRequestSentEvent event) {
        log.info("Nhận FRIEND_REQUEST: {} → {}", event.getSenderId(), event.getReceiverId());

        Notification notification = Notification.builder()
                .userId(event.getReceiverId())   // B nhận thông báo
                .type("FRIEND_REQUEST")
                .title("Lời mời kết bạn")
                .body(event.getSenderName() + " muốn kết bạn với bạn")
                .referenceId(event.getRequestId())
                .isRead(false)
                .expireAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        notificationRepository.save(notification);
    }

    // Nhận khi B chấp nhận lời mời của A
    @RabbitListener(queues = RabbitMQConfig.FRIEND_ACCEPTED_QUEUE)
    public void handleFriendAccepted(FriendAcceptedEvent event) {
        log.info("Nhận FRIEND_ACCEPTED: {} chấp nhận {}", event.getReceiverId(), event.getSenderId());

        Notification notification = Notification.builder()
                .userId(event.getSenderId())     // A nhận thông báo "B đã chấp nhận"
                .type("FRIEND_ACCEPTED")
                .title("Chấp nhận kết bạn")
                .body(event.getReceiverName() + " đã chấp nhận lời mời kết bạn")
                .referenceId(event.getRequestId())
                .isRead(false)
                .expireAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        notificationRepository.save(notification);
    }
}