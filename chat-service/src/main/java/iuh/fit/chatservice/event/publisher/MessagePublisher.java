package iuh.fit.chatservice.event.publisher;

import iuh.fit.chatservice.entity.Message;
import iuh.fit.chatservice.event.payload.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// event/publisher/MessagePublisher.java
@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public void publishMessageSent(Message saved, String conversationId) {
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId(saved.getId())
                .roomId(conversationId)
                .senderId(saved.getSenderId())
                .content(saved.getContent())
                .sentAt(saved.getCreatedAt())
                .build();

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
