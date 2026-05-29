package iuh.fit.authservice.event.publisher;

import iuh.fit.authservice.config.MediaProperties;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.UserRegisteredEvent;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MediaProperties mediaProperties;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "exchange", "test.exchange");
        ReflectionTestUtils.setField(authService, "routingKey", "user.registered");
    }

    @Test
    void register_duplicateEmail_throwsUserAlreadyExists() {
        RegisterRequest request = new RegisterRequest("a@b.com", "user1", "secret12");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throwsUserAlreadyExists() {
        RegisterRequest request = new RegisterRequest("a@b.com", "user1", "secret12");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(userRepository.existsByUsername("user1")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_success_savesAndPublishesEvent() {
        RegisterRequest request = new RegisterRequest("new@b.com", "newuser", "secret12");
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(mediaProperties.defaultAvatarUrl()).thenReturn("https://cdn.example.com/static/default-avatar.jpg");
        when(encoder.encode("secret12")).thenReturn("hashed");

        User saved = new User("new@b.com", "newuser", "hashed");
        saved.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(saved);

        authService.register(request);

        verify(encoder).encode("secret12");
        verify(userRepository).save(any(User.class));
        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"), eq("user.registered"), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(saved.getId().toString(), captor.getValue().getId());
        org.junit.jupiter.api.Assertions.assertEquals("new@b.com", captor.getValue().getEmail());
    }
}
