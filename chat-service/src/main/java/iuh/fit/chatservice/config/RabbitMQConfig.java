package iuh.fit.chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.chat.exchange}")
    private String chatExchangeName;

    @Value("${rabbitmq.chat.queue}")
    private String chatQueueName;

    @Value("${rabbitmq.chat.routing-key}")
    private String chatRoutingKey;

    @Value("${rabbitmq.presence.exchange:presence.exchange}")
    private String presenceExchangeName;

    @Value("${rabbitmq.presence.notify-queue:chat.presence.notify.queue}")
    private String presenceNotifyQueueName;

    @Value("${rabbitmq.presence.routing-key.notify:user.presence.notify}")
    private String presenceNotifyRoutingKey;

    @Value("${rabbitmq.internal.exchange:chat.internal}")
    private String internalExchangeName;

    @Value("${rabbitmq.internal.persist-queue:chat.persist.queue}")
    private String persistQueueName;

    @Value("${rabbitmq.internal.persist-routing-key:chat.message.created}")
    private String persistRoutingKey;

    @Value("${rabbitmq.internal.hydrate-queue:chat.hydrate.queue}")
    private String hydrateQueueName;

    @Value("${rabbitmq.internal.hydrate-routing-key:chat.history.load}")
    private String hydrateRoutingKey;

    @Value("${rabbitmq.internal.updated-routing-key:chat.message.updated}")
    private String updatedRoutingKey;

    @Value("${rabbitmq.internal.dlx:chat.internal.dlx}")
    private String internalDlxName;

    @Value("${rabbitmq.internal.persist-dlq:chat.persist.dlq}")
    private String persistDlqName;

    @Value("${rabbitmq.internal.persist-dlq-routing-key:chat.persist.dlq}")
    private String persistDlqRoutingKey;

    @Value("${rabbitmq.internal.hydrate-dlq:chat.hydrate.dlq}")
    private String hydrateDlqName;

    @Value("${rabbitmq.internal.hydrate-dlq-routing-key:chat.hydrate.dlq}")
    private String hydrateDlqRoutingKey;

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(chatExchangeName);
    }

    @Bean
    public Queue chatNotificationQueue() {
        return QueueBuilder.durable(chatQueueName).build();
    }

    @Bean
    public Binding chatNotificationBinding(Queue chatNotificationQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatNotificationQueue).to(chatExchange).with(chatRoutingKey);
    }

    @Bean
    public TopicExchange presenceExchange() {
        return new TopicExchange(presenceExchangeName);
    }

    @Bean
    public Queue presenceNotifyQueue() {
        return QueueBuilder.durable(presenceNotifyQueueName).build();
    }

    @Bean
    public Binding presenceNotifyBinding(Queue presenceNotifyQueue, TopicExchange presenceExchange) {
        return BindingBuilder.bind(presenceNotifyQueue).to(presenceExchange).with(presenceNotifyRoutingKey);
    }

    @Bean
    public TopicExchange chatInternalExchange() {
        return new TopicExchange(internalExchangeName);
    }

    @Bean
    public TopicExchange chatInternalDlx() {
        return new TopicExchange(internalDlxName);
    }

    @Bean
    public Queue chatPersistDlq() {
        return QueueBuilder.durable(persistDlqName).build();
    }

    @Bean
    public Queue chatHydrateDlq() {
        return QueueBuilder.durable(hydrateDlqName).build();
    }

    @Bean
    public Binding chatPersistDlqBinding(Queue chatPersistDlq, TopicExchange chatInternalDlx) {
        return BindingBuilder.bind(chatPersistDlq).to(chatInternalDlx).with(persistDlqRoutingKey);
    }

    @Bean
    public Binding chatHydrateDlqBinding(Queue chatHydrateDlq, TopicExchange chatInternalDlx) {
        return BindingBuilder.bind(chatHydrateDlq).to(chatInternalDlx).with(hydrateDlqRoutingKey);
    }

    @Bean
    public Queue chatPersistQueue() {
        return QueueBuilder.durable(persistQueueName)
                .deadLetterExchange(internalDlxName)
                .deadLetterRoutingKey(persistDlqRoutingKey)
                .build();
    }

    @Bean
    public Queue chatHydrateQueue() {
        return QueueBuilder.durable(hydrateQueueName)
                .deadLetterExchange(internalDlxName)
                .deadLetterRoutingKey(hydrateDlqRoutingKey)
                .build();
    }

    @Bean
    public Binding chatPersistCreatedBinding(Queue chatPersistQueue, TopicExchange chatInternalExchange) {
        return BindingBuilder.bind(chatPersistQueue).to(chatInternalExchange).with(persistRoutingKey);
    }

    @Bean
    public Binding chatPersistUpdatedBinding(Queue chatPersistQueue, TopicExchange chatInternalExchange) {
        return BindingBuilder.bind(chatPersistQueue).to(chatInternalExchange).with(updatedRoutingKey);
    }

    @Bean
    public Binding chatHydrateBinding(Queue chatHydrateQueue, TopicExchange chatInternalExchange) {
        return BindingBuilder.bind(chatHydrateQueue).to(chatInternalExchange).with(hydrateRoutingKey);
    }

    @Bean
    @Primary
    public MessageConverter messageConverter(ObjectMapper chatSpaceObjectMapper) {
        return new Jackson2JsonMessageConverter(chatSpaceObjectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
