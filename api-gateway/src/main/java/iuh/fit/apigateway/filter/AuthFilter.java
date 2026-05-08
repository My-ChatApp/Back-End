package iuh.fit.apigateway.filter;

import iuh.fit.apigateway.dto.response.AuthResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    public AuthFilter(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("http://localhost:8081")
                .build();
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getPath()
                .value();

        // public routes
        if (path.startsWith("/api/v1/auth/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            return unauthorized(exchange);
        }

        return webClient.post()
                .uri("/api/v1/auth/validate")
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(AuthResponse.class)

                .flatMap(authResponse -> {

                    if (!authResponse.isValid()) {
                        return unauthorized(exchange);
                    }

                    ServerHttpRequest request =
                            exchange.getRequest()
                                    .mutate()
                                    .header(
                                            "X-Email",
                                            authResponse.getEmail()
                                    )
                                    .build();

                    return chain.filter(
                            exchange.mutate()
                                    .request(request)
                                    .build()
                    );
                })

                .onErrorResume(
                        e -> unauthorized(exchange)
                );
    }

    private Mono<Void> unauthorized(
            ServerWebExchange exchange) {

        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}