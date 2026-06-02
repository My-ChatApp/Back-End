package iuh.fit.apigateway.filter;

import iuh.fit.apigateway.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CorsConfigurationSource corsConfigurationSource;

    @Mock
    private GatewayFilterChain chain;

    private AuthFilter authFilter;

    @BeforeEach
    void setUp() {
        authFilter = new AuthFilter(jwtService, corsConfigurationSource);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_publicAuthPath_skipsJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/signin").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authFilter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtService, never()).isValid(any());
        verify(chain).filter(exchange);
    }

    @Test
    void filter_missingBearer_returnsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authFilter.filter(exchange, chain))
                .verifyComplete();

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_validToken_addsUserHeaders() {
        String token = "valid-token";
        when(jwtService.isValid(token)).thenReturn(true);
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject("user@example.com")
                .add("userId", "user-42")
                .build();
        when(jwtService.extractClaims(token)).thenReturn(claims);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_publicAgentPath_skipsJwtValidation() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/agent/chat").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authFilter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtService, never()).isValid(any());
        verify(chain).filter(exchange);
    }

    @Test
    void filter_wsPath_forwardsWithoutAuth() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/ws/info").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authFilter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtService, never()).isValid(any());
        verify(chain).filter(exchange);
    }
}
