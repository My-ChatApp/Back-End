package iuh.fit.chatservice.event.publisher;

import iuh.fit.chatservice.event.payload.UserPresenceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PresencePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.presence.exchange:presence.exchange}")
    private String exchange;

    @Value("${rabbitmq.presence.routing-key.changed:user.presence.changed}")
    private String changedRoutingKey;

    public void publishPresenceChanged(String userId, boolean online) {
        rabbitTemplate.convertAndSend(
                exchange,
                changedRoutingKey,
                UserPresenceChangedEvent.builder()
                        .userId(userId)
                        .online(online)
                        .occurredAt(Instant.now())
                        .build());
    }
}
