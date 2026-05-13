package iuh.fit.chatservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatRabbitMQConfig {

    @Value("${rabbitmq.chat.exchange}")
    private String exchange;

    @Value("${rabbitmq.chat.queue}")
    private String queue;

    @Value("${rabbitmq.chat.routing-key}")
    private String routingKey;


    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue chatQueue() {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding chatBinding(Queue queue, TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(routingKey);
    }

    // Serialize payload thành JSON
    @Bean
    public MessageConverter chatMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate chatAmqpTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(chatMessageConverter());
        return template;
    }
}
