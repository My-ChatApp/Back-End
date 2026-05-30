package iuh.fit.chatservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "chat.space.enabled", havingValue = "true", matchIfMissing = true)
public class ValkeyConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        try {
            String pong = connectionFactory.getConnection().ping();
            log.info("Valkey/Redis connected: {}", pong);
        } catch (Exception e) {
            log.warn("Valkey/Redis not reachable at startup: {}", e.getMessage());
        }
        return template;
    }
}
