package iuh.fit.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    public static final String FRIEND_EXCHANGE             = "friend.exchange";
    public static final String FRIEND_REQUEST_QUEUE        = "noti.friend.request.queue";
    public static final String FRIEND_ACCEPTED_QUEUE       = "noti.friend.accepted.queue";

    @Value("${rabbitmq.mail.exchange}")
    private String mailExchange;

    @Value("${rabbitmq.mail.queue}")
    private String mailQueueName;

    @Value("${rabbitmq.mail.routing-key}")
    private String mailRoutingKey;

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
        return BindingBuilder.bind(friendRequestQueue())
                .to(friendExchange()).with("friend.request.sent");
    }

    @Bean
    public Binding friendAcceptedBinding() {
        return BindingBuilder.bind(friendAcceptedQueue())
                .to(friendExchange()).with("friend.request.accepted");
    }

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(mailExchange);
    }

    @Bean
    public Queue mailQueue() {
        return new Queue(mailQueueName, true);
    }

    @Bean
    public Binding mailBinding(Queue mailQueue, TopicExchange authExchange) {
        return BindingBuilder
                .bind(mailQueue)
                .to(authExchange)
                .with(mailRoutingKey);
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
