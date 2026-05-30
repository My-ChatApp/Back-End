package iuh.fit.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
@Conditional(RateLimitEnabledCondition.class)
public class RateLimitKeyResolvers {

    @Bean(name = "ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remote = exchange.getRequest().getRemoteAddress();
            if (remote != null && remote.getAddress() != null) {
                return Mono.just(remote.getAddress().getHostAddress());
            }
            return Mono.just("unknown");
        };
    }

    /** @Primary: default for GatewayAutoConfiguration#requestRateLimiterGatewayFilterFactory */
    @Bean(name = "userKeyResolver")
    @Primary
    public KeyResolver userKeyResolver(KeyResolver ipKeyResolver) {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-UserId");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            return ipKeyResolver.resolve(exchange);
        };
    }
}
