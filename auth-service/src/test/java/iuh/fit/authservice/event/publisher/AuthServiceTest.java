package iuh.fit.authservice.event.publisher;

import iuh.fit.authservice.config.MediaProperties;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.entity.OtpType;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.UserRegisteredEvent;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import iuh.fit.authservice.service.OtpService;
import iuh.fit.authservice.service.PendingRegistrationService;
import iuh.fit.authservice.util.PasswordPolicyValidator;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String VALID_PASSWORD = "Secret1!";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MediaProperties mediaProperties;

    @Mock
    private OtpService otpService;

    @Mock
    private PendingRegistrationService pendingRegistrationService;

    @InjectMocks
    private AuthService authService;

    private final PasswordPolicyValidator passwordPolicyValidator = new PasswordPolicyValidator();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "passwordPolicyValidator", passwordPolicyValidator);
        ReflectionTestUtils.setField(authService, "exchange", "test.exchange");
        ReflectionTestUtils.setField(authService, "routingKey", "user.registered");
        ReflectionTestUtils.setField(authService, "loginOtpRoutingKey", "auth.login-otp");
    }

    @Test
    void register_duplicateEmail_throwsUserAlreadyExists() {
        RegisterRequest request = new RegisterRequest("a@b.com", "user1", VALID_PASSWORD);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throwsUserAlreadyExists() {
        RegisterRequest request = new RegisterRequest("a@b.com", "user1", VALID_PASSWORD);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(userRepository.existsByUsername("user1")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_success_publishOtpEvent() {
        RegisterRequest request = new RegisterRequest("new@b.com", "newuser", VALID_PASSWORD);

        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(otpService.getExpiryMinutes()).thenReturn(5);
        when(otpService.sendOtp("new@b.com", OtpType.REGISTER)).thenReturn("123456");

        authService.register(request);

        verify(pendingRegistrationService).save(request);

        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("user.registered"),
                captor.capture()
        );

        UserRegisteredEvent event = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("new@b.com", event.getEmail());
        org.junit.jupiter.api.Assertions.assertEquals("123456", event.getOtp());
    }

    @Test
    void verifyRegistrationOtp_success_setsEmailVerified() {
        RegisterRequest pending = new RegisterRequest("new@b.com", "newuser", VALID_PASSWORD);
        when(otpService.verifyOtp("new@b.com", "123456", OtpType.REGISTER)).thenReturn(true);
        when(pendingRegistrationService.get("new@b.com")).thenReturn(pending);
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(encoder.encode(VALID_PASSWORD)).thenReturn("encoded");
        when(mediaProperties.defaultAvatarUrl()).thenReturn("http://avatar");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = authService.verifyRegistrationOtp("new@b.com", "123456");

        assertTrue(user.isEmailVerified());
        verify(pendingRegistrationService).delete("new@b.com");
    }
}
