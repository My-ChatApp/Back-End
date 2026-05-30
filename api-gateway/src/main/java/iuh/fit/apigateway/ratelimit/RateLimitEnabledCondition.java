package iuh.fit.apigateway.ratelimit;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class RateLimitEnabledCondition extends AllNestedConditions {

    public RateLimitEnabledCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(prefix = "gateway.ratelimit", name = "enabled", havingValue = "true")
    static class OnRateLimitEnabled {
    }
}
