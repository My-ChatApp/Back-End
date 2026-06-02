package iuh.fit.apigateway.filter;

import io.jsonwebtoken.Claims;
import iuh.fit.apigateway.service.JwtService;

import iuh.fit.common.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import org.springframework.core.Ordered;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;

import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final DefaultCorsProcessor corsProcessor = new DefaultCorsProcessor();

    public AuthFilter(JwtService jwtService, CorsConfigurationSource corsConfigurationSource) {
        this.jwtService = jwtService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        // 🔴 LOG ĐẦU TIÊN - để chắc chắn filter đang chạy
        log.info("\n\n===== [AuthFilter] NEW REQUEST =====");

        String path = exchange.getRequest()
                .getPath()
                .value();

        log.info("[AuthFilter] Request path: {}", path);

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        // Public routes
        if (isPublicPath(path)) {
            log.info("[AuthFilter] Public route, skipping auth: {}", path);
            return chain.filter(exchange);
        }

        // SockJS HTTP transport only (/ws/info, xhr, …) — NOT the security boundary.
        // JWT is enforced on STOMP CONNECT in chat-service (StompAuthChannelInterceptor).
        if (path.startsWith("/ws")) {
            return forwardWebSocketHandshake(exchange, chain);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        log.info("[AuthFilter] Authorization header: {}", authHeader);

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {
            log.error("[AuthFilter] Missing or invalid Authorization header");
            return unauthorized(exchange);
        }

        try {
            String token = authHeader.substring(7);
            log.info("[AuthFilter] Token extracted");

            if (!jwtService.isValid(token)) {
                log.error("[AuthFilter] JWT validation failed");
                return unauthorized(exchange);
            }

            log.info("[AuthFilter] JWT validation passed");
            Claims claims = jwtService.extractClaims(token);
            String userId = claims.get("userId").toString();
            String email = claims.getSubject();

            log.info("[AuthFilter] userId: {}, email: {}", userId, email);

            // 👉 CREATE CURRENT USER
            CurrentUser currentUser =
                    new CurrentUser(userId, email);


            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            currentUser,
                            null,
                            List.of()
                    );

            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .header("X-UserId", userId)
                    .header("X-Email", email)
                    .build();

            return chain.filter(
                            exchange.mutate()
                                    .request(request)
                                    .build()
                    )
                    .contextWrite(
                            ReactiveSecurityContextHolder.withAuthentication(authentication)
                    );

        } catch (Exception e) {
            log.error("[AuthFilter] Exception occurred", e);
            return unauthorized(exchange);
        }
    }

    private static boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/") || path.startsWith("/api/agent/");
    }

    /**
     * SockJS opens several plain HTTP requests before STOMP; they cannot send
     * {@code Authorization}. Forward transport to chat-service; reject anonymous
     * messaging at STOMP CONNECT instead.
     */
    private Mono<Void> forwardWebSocketHandshake(ServerWebExchange exchange,
                                                 GatewayFilterChain chain) {
        log.debug("[AuthFilter] SockJS/STOMP transport {}, forwarding (auth at STOMP CONNECT)", 
                exchange.getRequest().getPath().value());
        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        log.error("[AuthFilter] Returning UNAUTHORIZED");
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        applyCorsIfNeeded(exchange);
        return exchange.getResponse().setComplete();
    }

    /** Without this, browsers report a CORS error instead of 401 for cross-origin calls. */
    private void applyCorsIfNeeded(ServerWebExchange exchange) {
        if (!CorsUtils.isCorsRequest(exchange.getRequest())) {
            return;
        }
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(exchange);
        if (config != null) {
            corsProcessor.process(config, exchange);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}