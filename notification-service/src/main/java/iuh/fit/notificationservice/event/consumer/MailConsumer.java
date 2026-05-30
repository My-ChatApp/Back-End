package iuh.fit.notificationservice.event.consumer;

import iuh.fit.notificationservice.event.payload.UserRegisteredEvent;
import iuh.fit.notificationservice.service.SesMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailConsumer {

    private final SesMailService sesMailService;

    @Value("${mychatapp.mail.from:}")
    private String mailFrom;

    @Value("${mychatapp.mail.registration-enabled:false}")
    private boolean registrationMailEnabled;

    @RabbitListener(queues = "${rabbitmq.mail.queue}")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received event: {}", event);

        if (!registrationMailEnabled) {
            log.info("Skip welcome email for {} (registration mail disabled)", event.getEmail());
            return;
        }

        try {
            sesMailService.sendText(
                    event.getEmail(),
                    "Chào mừng bạn!",
                    "Xin chào " + event.getUsername() + ", cảm ơn bạn đã đăng ký!");
            log.info("Email sent from {} to: {}", mailFrom, event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", event.getEmail(), e.getMessage(), e);
            throw new IllegalStateException("Gửi mail SES thất bại", e);
        }
    }
}
