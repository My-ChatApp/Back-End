package iuh.fit.apigateway.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "gateway.ratelimit")
public class GatewayRateLimitProperties {

    /** Fail-open: when false, no RequestRateLimiter and no Redis required. */
    private boolean enabled = false;

    private int authReplenish = 5;
    private int authBurst = 10;

    private int mediaReplenish = 10;
    private int mediaBurst = 20;

    private int defaultReplenish = 50;
    private int defaultBurst = 100;

    private int wsReplenish = 50;
    private int wsBurst = 100;
}
