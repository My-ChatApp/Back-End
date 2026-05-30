package iuh.fit.userservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    public static final String FRIEND_EXCHANGE = "friend.exchange";
    public static final String FRIEND_REQUEST_QUEUE = "friend.request.queue";
    public static final String FRIEND_ACCEPTED_QUEUE = "friend.accepted.queue";
    public static final String FRIEND_REQUEST_ROUTING_KEY = "friend.request.sent";
    public static final String FRIEND_ACCEPTED_ROUTING_KEY = "friend.request.accepted";

    public static final String PRESENCE_EXCHANGE = "presence.exchange";
    public static final String PRESENCE_CHANGED_QUEUE = "user.presence.changed.queue";
    public static final String PRESENCE_CHANGED_ROUTING_KEY = "user.presence.changed";
    public static final String PRESENCE_NOTIFY_ROUTING_KEY = "user.presence.notify";

    @Value("${rabbitmq.auth.exchange}")
    private String authExchangeName;

    @Value("${rabbitmq.auth.queue}")
    private String profileCreateQueueName;

    @Value("${rabbitmq.auth.routing-key}")
    private String authRoutingKey;

    @Bean
    public TopicExchange authTopicExchange() {
        return new TopicExchange(authExchangeName);
    }

    @Bean
    public Queue profileCreateQueue() {
        return new Queue(profileCreateQueueName, true);
    }

    @Bean
    public Binding profileCreateBinding(Queue profileCreateQueue, TopicExchange authTopicExchange) {
        return BindingBuilder
                .bind(profileCreateQueue)
                .to(authTopicExchange)
                .with(authRoutingKey);
    }

    @Bean
    public TopicExchange friendExchange() {
        return new TopicExchange(FRIEND_EXCHANGE);
    }

    @Bean
    public Queue friendRequestQueue() {
        return QueueBuilder.durable(FRIEND_REQUEST_QUEUE).build();
    }

    @Bean
    public Queue friendAcceptedQueue() {
        return QueueBuilder.durable(FRIEND_ACCEPTED_QUEUE).build();
    }

    @Bean
    public Binding friendRequestBinding() {
        return BindingBuilder
                .bind(friendRequestQueue())
                .to(friendExchange())
                .with(FRIEND_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding friendAcceptedBinding() {
        return BindingBuilder
                .bind(friendAcceptedQueue())
                .to(friendExchange())
                .with(FRIEND_ACCEPTED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange presenceExchange() {
        return new TopicExchange(PRESENCE_EXCHANGE);
    }

    @Bean
    public Queue presenceChangedQueue() {
        return QueueBuilder.durable(PRESENCE_CHANGED_QUEUE).build();
    }

    @Bean
    public Binding presenceChangedBinding() {
        return BindingBuilder
                .bind(presenceChangedQueue())
                .to(presenceExchange())
                .with(PRESENCE_CHANGED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
