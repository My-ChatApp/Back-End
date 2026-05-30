package iuh.fit.apigateway.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();
    private final String secret = "test-secret-key-at-least-32-bytes-long!!";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        jwtService.init();
    }

    @Test
    void isValid_validToken_returnsTrue() {
        String token = buildToken("user-1", "user@example.com");

        assertTrue(jwtService.isValid(token));
        assertEquals("user@example.com", jwtService.extractClaims(token).getSubject());
        assertEquals("user-1", jwtService.extractClaims(token).get("userId").toString());
    }

    @Test
    void isValid_invalidToken_returnsFalse() {
        assertFalse(jwtService.isValid("not.a.valid.jwt"));
    }

    private String buildToken(String userId, String email) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
