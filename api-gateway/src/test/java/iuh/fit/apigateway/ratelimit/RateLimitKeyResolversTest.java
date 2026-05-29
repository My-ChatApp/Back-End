package iuh.fit.apigateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyResolversTest {

  private final RateLimitKeyResolvers config = new RateLimitKeyResolvers();

  @Test
  void ipKeyResolver_usesRemoteAddress() {
    KeyResolver resolver = config.ipKeyResolver();
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/signin")
        .remoteAddress(new java.net.InetSocketAddress("203.0.113.10", 12345))
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    String key = resolver.resolve(exchange).block();
    assertThat(key).isEqualTo("203.0.113.10");
  }

  @Test
  void userKeyResolver_prefersXUserId() {
    KeyResolver ip = config.ipKeyResolver();
    KeyResolver user = config.userKeyResolver(ip);
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/media/x")
        .header("X-UserId", "user-42")
        .remoteAddress(new java.net.InetSocketAddress("203.0.113.10", 12345))
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    String key = user.resolve(exchange).block();
    assertThat(key).isEqualTo("user-42");
  }

  @Test
  void userKeyResolver_fallsBackToIp() {
    KeyResolver ip = config.ipKeyResolver();
    KeyResolver user = config.userKeyResolver(ip);
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/media/x")
        .remoteAddress(new java.net.InetSocketAddress("203.0.113.10", 12345))
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    String key = user.resolve(exchange).block();
    assertThat(key).isEqualTo("203.0.113.10");
  }
}
