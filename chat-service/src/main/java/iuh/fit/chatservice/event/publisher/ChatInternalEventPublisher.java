package iuh.fit.chatservice.event.publisher;

import iuh.fit.chatservice.event.payload.ChatMessageCreatedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageReactionsUpdatedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageDeletedEvent;
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

    @Value("${rabbitmq.internal.deleted-routing-key:chat.message.deleted}")
    private String deletedRoutingKey;

    @Value("${rabbitmq.internal.reactions-routing-key:chat.message.reactions.updated}")
    private String reactionsRoutingKey;

    public void publishMessageCreated(ChatMessageCreatedEvent event) {
        rabbitTemplate.convertAndSend(internalExchange, persistRoutingKey, event);
    }

    public void publishMessageUpdated(ChatMessageUpdatedEvent event) {
        rabbitTemplate.convertAndSend(internalExchange, updatedRoutingKey, event);
    }

    public void publishMessageDeleted(ChatMessageDeletedEvent event) {
        rabbitTemplate.convertAndSend(internalExchange, deletedRoutingKey, event);
    }

    public void publishMessageReactionsUpdated(ChatMessageReactionsUpdatedEvent event) {
        rabbitTemplate.convertAndSend(internalExchange, reactionsRoutingKey, event);
    }

    public void publishHistoryLoadRequested(ConversationHistoryLoadRequestedEvent event) {
        rabbitTemplate.convertAndSend(internalExchange, hydrateRoutingKey, event);
    }
}
