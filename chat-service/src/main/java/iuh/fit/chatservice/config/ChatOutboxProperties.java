package iuh.fit.chatservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat.outbox")
public class ChatOutboxProperties {

    private boolean enabled = true;
    private long pollIntervalMs = 3000;
    private int batchSize = 50;
    private int maxPublishAttempts = 5;
    private long retryInitialIntervalMs = 2000;
    private long retryMaxIntervalMs = 60000;
    private double retryMultiplier = 2.0;
}
