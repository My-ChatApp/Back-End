package iuh.fit.chatservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty() || secret.contains("${")) {
            log.error("[chat JwtService] JWT_SECRET not configured");
        } else {
            log.info("[chat JwtService] JWT secret loaded");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("[chat JwtService] invalid token: {}", e.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        Object userId = extractClaims(token).get("userId");
        return userId != null ? userId.toString() : null;
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }
}
