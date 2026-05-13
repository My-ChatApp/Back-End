package iuh.fit.apigateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        log.info("[JwtService] Initializing with JWT secret: {}", secret != null ? "***" : "NULL");
        if (secret == null || secret.isEmpty() || secret.contains("${")) {
            log.error("[JwtService] ❌ JWT_SECRET not properly configured! secret={}", secret);
        } else {
            log.info("[JwtService] ✅ JWT_SECRET loaded successfully");
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
            Claims claims = extractClaims(token);
            log.info("[JwtService] Claims extracted successfully: {}", claims);
            return true;

        } catch (Exception e) {
            log.error("[JwtService] JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
