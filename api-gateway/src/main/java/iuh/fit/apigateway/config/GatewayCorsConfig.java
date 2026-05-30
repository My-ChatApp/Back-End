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
 * Spring WebFlux Security blocks cross-origin requests unless CORS is enabled here
 * (in addition to spring.cloud.gateway globalcors in application.yml).
 */
@Configuration
public class GatewayCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:http://localhost:3000}") String allowedOriginPatterns
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(
                Arrays.stream(allowedOriginPatterns.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
        );
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
