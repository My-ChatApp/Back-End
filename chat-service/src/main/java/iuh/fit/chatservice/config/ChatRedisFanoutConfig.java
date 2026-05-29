package iuh.fit.chatservice.config;

import iuh.fit.chatservice.service.ChatRealtimeBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.space.redis-fanout-enabled", havingValue = "true", matchIfMissing = true)
public class ChatRedisFanoutConfig {

    private final ChatRedisFanoutListener fanoutListener;

    @Bean
    RedisMessageListenerContainer chatFanoutListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        MessageListenerAdapter adapter = new MessageListenerAdapter(fanoutListener, "handleRedisFanoutMessage");
        container.addMessageListener(adapter, new PatternTopic("chat:fanout:*"));
        return container;
    }
}
