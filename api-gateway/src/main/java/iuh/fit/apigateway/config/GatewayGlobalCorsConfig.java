package iuh.fit.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * Gateway-native CORS for {@code /api/**} (including proxied agent-service).
 * Spring Security's {@code CorsWebFilter} alone can miss {@code Access-Control-Allow-Origin}
 * on some proxied responses after {@code removeResponseHeader} strips upstream CORS.
 * {@code /ws/**} is not covered here — chat-service SockJS sets its own CORS.
 * Duplicates with {@link GatewayCorsConfig} are deduped in application.yml.
 */
@Configuration
public class GatewayGlobalCorsConfig {

    @Bean
    public GlobalCorsProperties apiGlobalCorsProperties(
            @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:http://localhost:3000,https://*.vercel.app,https://chat.oeb20412.com}") String allowedOriginPatterns
    ) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(parsePatterns(allowedOriginPatterns));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        cors.setExposedHeaders(List.of("Authorization"));
        cors.setMaxAge(3600L);

        GlobalCorsProperties properties = new GlobalCorsProperties();
        properties.getCorsConfigurations().put("/api/**", cors);
        return properties;
    }

    private static List<String> parsePatterns(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
