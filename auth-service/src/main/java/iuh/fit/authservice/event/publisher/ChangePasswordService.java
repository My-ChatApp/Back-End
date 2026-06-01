package iuh.fit.authservice.event.publisher;

import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.ChangePasswordEvent;
import iuh.fit.authservice.event.payload.UserRegisteredEvent;
import iuh.fit.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {
    private final String exchange = "auth.exchange";
    private final String routingKey = "auth.change-password";
    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;

    public void publishChangePasswordEvent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        // Tạo payload cho sự kiện
        ChangePasswordEvent event = new ChangePasswordEvent(email);

        // Gửi sự kiện đến RabbitMQ
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                event
        );
    }
}
