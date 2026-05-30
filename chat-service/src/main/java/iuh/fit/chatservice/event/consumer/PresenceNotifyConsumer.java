package iuh.fit.chatservice.event.consumer;

import iuh.fit.chatservice.event.payload.UserPresenceNotifyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceNotifyConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "${rabbitmq.presence.notify-queue}")
    public void handlePresenceNotify(UserPresenceNotifyEvent event) {
        if (event == null || event.getUserId() == null || event.getTargetUserId() == null) {
            throw new IllegalArgumentException("UserPresenceNotifyEvent thiếu trường bắt buộc");
        }

        log.debug("Fan-out presence {} → {} (online={})",
                event.getUserId(), event.getTargetUserId(), event.isOnline());

        messagingTemplate.convertAndSend(
                "/topic/presence/" + event.getTargetUserId(),
                event);
    }
}
