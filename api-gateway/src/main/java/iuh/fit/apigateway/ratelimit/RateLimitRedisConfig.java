package iuh.fit.apigateway.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration
@Conditional(RateLimitEnabledCondition.class)
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
public class RateLimitRedisConfig {

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory rateLimitRedisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password
    ) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        if (StringUtils.hasText(password)) {
            standalone.setPassword(password);
        }
        return new LettuceConnectionFactory(standalone);
    }

    @Bean
    @Primary
    public ReactiveStringRedisTemplate rateLimitReactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory rateLimitRedisConnectionFactory
    ) {
        return new ReactiveStringRedisTemplate(rateLimitRedisConnectionFactory);
    }

    /** @Primary: required by GatewayAutoConfiguration#requestRateLimiterGatewayFilterFactory */
    @Bean(name = "defaultRedisRateLimiter")
    @Primary
    public RedisRateLimiter defaultRedisRateLimiter(GatewayRateLimitProperties properties) {
        return new RedisRateLimiter(
                properties.getDefaultReplenish(),
                properties.getDefaultBurst(),
                1
        );
    }

    @Bean(name = "authRedisRateLimiter")
    public RedisRateLimiter authRedisRateLimiter(GatewayRateLimitProperties properties) {
        return new RedisRateLimiter(
                properties.getAuthReplenish(),
                properties.getAuthBurst(),
                1
        );
    }

    @Bean(name = "mediaRedisRateLimiter")
    public RedisRateLimiter mediaRedisRateLimiter(GatewayRateLimitProperties properties) {
        return new RedisRateLimiter(
                properties.getMediaReplenish(),
                properties.getMediaBurst(),
                1
        );
    }

    @Bean(name = "wsRedisRateLimiter")
    public RedisRateLimiter wsRedisRateLimiter(GatewayRateLimitProperties properties) {
        return new RedisRateLimiter(
                properties.getWsReplenish(),
                properties.getWsBurst(),
                1
        );
    }
}
