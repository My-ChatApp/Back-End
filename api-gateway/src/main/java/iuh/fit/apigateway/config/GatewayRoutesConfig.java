package iuh.fit.apigateway.config;

import iuh.fit.apigateway.ratelimit.GatewayRateLimitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Value("${AUTH_SERVICE_URI:http://localhost:8081}")
    private String authServiceUri;

    @Value("${CHAT_SERVICE_URI:http://localhost:8082}")
    private String chatServiceUri;

    @Value("${USER_SERVICE_URI:http://localhost:8083}")
    private String userServiceUri;

    @Value("${NOTIFICATION_SERVICE_URI:http://localhost:8084}")
    private String notificationServiceUri;

    @Value("${MEDIA_SERVICE_URI:http://localhost:8085}")
    private String mediaServiceUri;

    @Value("${AGENT_SERVICE_URI:http://localhost:8088}")
    private String agentServiceUri;

    @Bean
    public RouteLocator gatewayRouteLocator(
            RouteLocatorBuilder builder,
            GatewayRateLimitProperties rateLimit,
            @Autowired(required = false) @Qualifier("authRedisRateLimiter") RedisRateLimiter authRedisRateLimiter,
            @Autowired(required = false) @Qualifier("defaultRedisRateLimiter") RedisRateLimiter defaultRedisRateLimiter,
            @Autowired(required = false) @Qualifier("mediaRedisRateLimiter") RedisRateLimiter mediaRedisRateLimiter,
            @Autowired(required = false) @Qualifier("wsRedisRateLimiter") RedisRateLimiter wsRedisRateLimiter,
            @Autowired(required = false) @Qualifier("ipKeyResolver") KeyResolver ipKeyResolver,
            @Autowired(required = false) @Qualifier("userKeyResolver") KeyResolver userKeyResolver
    ) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        routes.route("auth-health", r -> r.path("/api/auth/health").uri(authServiceUri));

        if (rateLimit.isEnabled()) {
            routes.route("auth-service", r -> r.path("/api/auth/**")
                    .filters(f -> rateLimit(f, authRedisRateLimiter, ipKeyResolver))
                    .uri(authServiceUri));

            routes.route("auth-users", r -> r.path("/api/users/**")
                    .filters(f -> rateLimit(f, defaultRedisRateLimiter, userKeyResolver))
                    .uri(authServiceUri));

            routes.route("chat-service", r -> r.path("/api/chat/**")
                    .filters(f -> rateLimit(f, defaultRedisRateLimiter, userKeyResolver))
                    .uri(chatServiceUri));

            routes.route("chat-conversations", r -> r.path("/api/conversations/**")
                    .filters(f -> rateLimit(f, defaultRedisRateLimiter, userKeyResolver))
                    .uri(chatServiceUri));

            routes.route("chat-websocket", r -> r.path("/ws/**")
                    .filters(f -> rateLimit(f, wsRedisRateLimiter, ipKeyResolver))
                    .uri(chatServiceUri));

            routes.route("user-service", r -> r.path("/api/profiles/**")
                    .filters(f -> rateLimit(f, defaultRedisRateLimiter, userKeyResolver))
                    .uri(userServiceUri));

            routes.route("notification-service", r -> r.path("/api/notifications/**")
                    .filters(f -> rateLimit(f, defaultRedisRateLimiter, userKeyResolver))
                    .uri(notificationServiceUri));

            routes.route("friend-service", r -> r.path("/api/friends/**")
                    .filters(f -> rateLimit(f, defaultRedisRateLimiter, userKeyResolver))
                    .uri(userServiceUri));

            routes.route("media-service", r -> r.path("/api/media/**")
                    .filters(f -> rateLimit(f, mediaRedisRateLimiter, userKeyResolver))
                    .uri(mediaServiceUri));

            routes.route("agent-health", r -> r.path("/api/agent/health")
                    .filters(f -> f.rewritePath("/api/agent/health", "/health"))
                    .uri(agentServiceUri));

            routes.route("agent-chat", r -> r.path("/api/agent/chat")
                    .filters(f -> rateLimit(
                            f.rewritePath("/api/agent/chat", "/api/chat"),
                            defaultRedisRateLimiter,
                            userKeyResolver))
                    .uri(agentServiceUri));
        } else {
            routes.route("auth-service", r -> r.path("/api/auth/**").uri(authServiceUri));
            routes.route("auth-users", r -> r.path("/api/users/**").uri(authServiceUri));
            routes.route("chat-service", r -> r.path("/api/chat/**").uri(chatServiceUri));
            routes.route("chat-conversations", r -> r.path("/api/conversations/**").uri(chatServiceUri));
            routes.route("chat-websocket", r -> r.path("/ws/**").uri(chatServiceUri));
            routes.route("user-service", r -> r.path("/api/profiles/**").uri(userServiceUri));
            routes.route("notification-service", r -> r.path("/api/notifications/**").uri(notificationServiceUri));
            routes.route("friend-service", r -> r.path("/api/friends/**").uri(userServiceUri));
            routes.route("media-service", r -> r.path("/api/media/**").uri(mediaServiceUri));

            routes.route("agent-health", r -> r.path("/api/agent/health")
                    .filters(f -> f.rewritePath("/api/agent/health", "/health"))
                    .uri(agentServiceUri));

            routes.route("agent-chat", r -> r.path("/api/agent/chat")
                    .filters(f -> f.rewritePath("/api/agent/chat", "/api/chat"))
                    .uri(agentServiceUri));
        }

        return routes.build();
    }

    private UriSpec rateLimit(
            GatewayFilterSpec spec,
            RedisRateLimiter rateLimiter,
            KeyResolver keyResolver
    ) {
        return spec.requestRateLimiter(config -> config
                .setRateLimiter(rateLimiter)
                .setKeyResolver(keyResolver));
    }
}
