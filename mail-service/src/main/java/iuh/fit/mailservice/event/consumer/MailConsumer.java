package iuh.fit.mailservice.event.consumer;

import iuh.fit.mailservice.event.payload.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailConsumer {

    private final JavaMailSender mailSender;

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void handleUserRegistered(UserRegisteredEvent event) {

        log.info("Received event: {}", event);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(event.getEmail());
        mail.setSubject("Chào mừng bạn!");
        mail.setText("Xin chào " + event.getUsername()
                + ", cảm ơn bạn đã đăng ký!");

        mailSender.send(mail);

        log.info("Email sent to: {}", event.getEmail());
    }
}