package iuh.fit.chatservice.event.publisher;

import iuh.fit.chatservice.event.payload.ChatMessageCreatedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageUpdatedEvent;
import iuh.fit.chatservice.event.payload.ConversationHistoryLoadRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatInternalEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.internal.exchange:chat.internal}")
    private String internalExchange;

    @Value("${rabbitmq.internal.persist-routing-key:chat.message.created}")
    private String persistRoutingKey;

    @Value("${rabbitmq.internal.hydrate-routing-key:chat.history.load}")
    private String hydrateRoutingKey;

    @Value("${rabbitmq.internal.updated-routing-key:chat.message.updated}")
    private String updatedRoutingKey;

    public void publishMessageCreated(ChatMessageCreatedEvent event) {
        rabbitTemplate.convertAndSend(internalExchange, persistRoutingKey, event);
    }

    public void publishMessageUpdated(ChatMessageUpdatedEvent event) {
        rabbitTemplate.convertAndSend(internalExchange, updatedRoutingKey, event);
    }

    public void publishHistoryLoadRequested(ConversationHistoryLoadRequestedEvent event) {
        rabbitTemplate.convertAndSend(internalExchange, hydrateRoutingKey, event);
    }
}
