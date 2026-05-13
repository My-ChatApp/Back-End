package iuh.fit.apigateway.filter;

import io.jsonwebtoken.Claims;
import iuh.fit.apigateway.service.JwtService;

import iuh.fit.common.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import org.springframework.core.Ordered;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public AuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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

        // Public routes
        if (path.startsWith("/api/v1/auth/")) {
            log.info("[AuthFilter] Public route, skipping auth");
            return chain.filter(exchange);
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

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        log.error("[AuthFilter] Returning UNAUTHORIZED");
        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}