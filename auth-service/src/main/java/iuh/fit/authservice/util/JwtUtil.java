package iuh.fit.authservice.util;


import iuh.fit.authservice.security.CustomUserDetails;
import iuh.fit.authservice.service.CustomUserDetailsService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        log.info("[JwtUtil] Initializing with JWT secret: {}", jwtSecret != null ? "***" : "NULL");
        if (jwtSecret == null || jwtSecret.isEmpty() || jwtSecret.contains("${")) {
            log.error("[JwtUtil] ❌ JWT_SECRET not properly configured! secret={}", jwtSecret);
        } else {
            log.info("[JwtUtil] ✅ JWT_SECRET loaded successfully, expiration={}", jwtExpirationMs);
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails) {
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        String token = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId",customUserDetails.getUserid())
                .claim("username",customUserDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key)
                .compact();
        
        log.info("[JwtUtil] Token generated for user: {}, token length: {}", userDetails.getUsername(), token.length());
        return token;
    }

    public String getUserFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }
}