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

    @Value("${rabbitmq.password-reset.queue:auth.password-reset.queue}")
    private String passwordResetQueueName;

    @Value("${rabbitmq.password-reset.routing-key:auth.password-reset}")
    private String passwordResetRoutingKey;

    @Value("${rabbitmq.password-reset-confirmation.queue:auth.password-reset-confirmed.queue}")
    private String passwordResetConfirmationQueueName;

    @Value("${rabbitmq.password-reset-confirmation.routing-key:auth.password-reset-confirmed}")
    private String passwordResetConfirmationRoutingKey;

    @Value("${rabbitmq.login-otp.queue:auth.login-otp.queue}")
    private String loginOtpQueueName;

    @Value("${rabbitmq.login-otp.routing-key:auth.login-otp}")
    private String loginOtpRoutingKey;

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
    public Queue passwordResetQueue() {
        return QueueBuilder.durable(passwordResetQueueName).build();
    }

    @Bean
    public Binding passwordResetBinding(Queue passwordResetQueue, TopicExchange authExchange) {
        return BindingBuilder
                .bind(passwordResetQueue)
                .to(authExchange)
                .with(passwordResetRoutingKey);
    }

    @Bean
    public Queue passwordResetConfirmationQueue() {
        return QueueBuilder.durable(passwordResetConfirmationQueueName).build();
    }

    @Bean
    public Binding passwordResetConfirmationBinding(Queue passwordResetConfirmationQueue, TopicExchange authExchange) {
        return BindingBuilder
                .bind(passwordResetConfirmationQueue)
                .to(authExchange)
                .with(passwordResetConfirmationRoutingKey);
    }

    @Bean
    public Queue loginOtpQueue() {
        return QueueBuilder.durable(loginOtpQueueName).build();
    }

    @Bean
    public Binding loginOtpBinding(Queue loginOtpQueue, TopicExchange authExchange) {
        return BindingBuilder
                .bind(loginOtpQueue)
                .to(authExchange)
                .with(loginOtpRoutingKey);
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
