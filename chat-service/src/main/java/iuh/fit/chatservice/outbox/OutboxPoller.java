package iuh.fit.chatservice.outbox;

import iuh.fit.chatservice.config.ChatOutboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxService outboxService;
    private final ChatOutboxProperties outboxProperties;

    @Scheduled(fixedDelayString = "${chat.outbox.poll-interval-ms:3000}")
    public void poll() {
        if (!outboxProperties.isEnabled()) {
            return;
        }
        outboxService.processPendingBatch();
    }
}
