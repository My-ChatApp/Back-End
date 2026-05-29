package iuh.fit.authservice.event.publisher;

import iuh.fit.authservice.config.MediaProperties;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.UserRegisteredEvent;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final MediaProperties mediaProperties;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        User user = new User(
                request.getEmail(),
                request.getUsername(),
                encoder.encode(request.getPassword())
        );
        user.applyDefaultAvatar(
                mediaProperties.defaultAvatarUrl(),
                MediaProperties.DEFAULT_AVATAR_KEY);

        User newUser = userRepository.save(user);

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                new UserRegisteredEvent(
                        newUser.getId().toString(),
                        newUser.getEmail(),
                        newUser.getUsername()
                )
        );
    }
}
