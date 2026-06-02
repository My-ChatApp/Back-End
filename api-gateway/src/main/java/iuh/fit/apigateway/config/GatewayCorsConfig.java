package iuh.fit.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS for REST routes via Spring Security's CorsWebFilter.
 * {@code /ws/**} is excluded — chat-service SockJS sets its own CORS headers; gateway + upstream
 * both emitting {@code Access-Control-Allow-Origin} breaks the browser (duplicate values).
 * Do not also enable spring.cloud.gateway globalcors.
 */
@Configuration
public class GatewayCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:http://localhost:3000,https://*.vercel.app,https://chat.oeb20412.com}") String allowedOriginPatterns
    ) {
        CorsConfiguration apiCors = new CorsConfiguration();
        apiCors.setAllowedOriginPatterns(
                Arrays.stream(allowedOriginPatterns.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
        );
        apiCors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        apiCors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        apiCors.setExposedHeaders(List.of("Authorization"));
        apiCors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource delegate = new UrlBasedCorsConfigurationSource();
        delegate.registerCorsConfiguration("/**", apiCors);

        // null → CorsWebFilter passes /ws/** through unchanged; chat-service SockJS sets CORS.
        return exchange -> {
            String path = exchange.getRequest().getPath().pathWithinApplication().value();
            if (path.startsWith("/ws")) {
                return null;
            }
            return delegate.getCorsConfiguration(exchange);
        };
    }
}
