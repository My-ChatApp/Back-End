package iuh.fit.friendservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "friend.exchange";
    public static final String FRIEND_REQUEST_QUEUE = "friend.request.queue";
    public static final String FRIEND_ACCEPTED_QUEUE = "friend.accepted.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
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
                .to(exchange())
                .with("friend.request.sent");
    }

    @Bean
    public Binding friendAcceptedBinding() {
        return BindingBuilder
                .bind(friendAcceptedQueue())
                .to(exchange())
                .with("friend.request.accepted");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}