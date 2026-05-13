package iuh.fit.authservice.event.publisher;

import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.UserRegisteredEvent;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = new User(
                request.getEmail(),
                request.getUsername(),
                encoder.encode(request.getPassword())
        );

        User newUser = userRepository.save(user);

        System.out.println("User registered: " + newUser);
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                new UserRegisteredEvent(
                        newUser.getId(),
                        newUser.getEmail(),
                        newUser.getUsername()
                )
        );
    }
}