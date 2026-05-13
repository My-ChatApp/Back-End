package iuh.fit.userservice.event.consumer;

import iuh.fit.userservice.entity.UserProfile;
import iuh.fit.userservice.event.payload.UserRegisteredEvent;
import iuh.fit.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileConsumer {

    private final UserProfileRepository userProfileRepository;

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received event: {}", event);

        userProfileRepository.save(
                new UserProfile(
                        event.getId(),
                        event.getUsername()
                )
        );

        log.info("User profile created for userId: {}", event.getId());
    }
}