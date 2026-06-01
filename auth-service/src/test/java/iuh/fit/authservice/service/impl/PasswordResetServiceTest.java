package iuh.fit.authservice.service.impl;

import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.repository.UserRepository;
import iuh.fit.authservice.util.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private final PasswordPolicyValidator passwordPolicyValidator = new PasswordPolicyValidator();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "passwordPolicyValidator", passwordPolicyValidator);
        ReflectionTestUtils.setField(passwordResetService, "exchange", "auth.exchange");
        ReflectionTestUtils.setField(passwordResetService, "passwordResetRoutingKey", "auth.password-reset");
        ReflectionTestUtils.setField(passwordResetService, "tokenExpiryMinutes", 15);
        ReflectionTestUtils.setField(passwordResetService, "resetLinkBase", "http://localhost/reset");
        ReflectionTestUtils.setField(passwordResetService, "maxAttempts", 3);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void resetPassword_weakPassword_incrementsAttempts() {
        String token = "token-abc";
        User user = new User("user@b.com", "user1", "hash");

        when(valueOperations.get("pwd_reset:" + token)).thenReturn("user@b.com");
        when(valueOperations.get("pwd_reset_attempts:" + token)).thenReturn(null);
        when(valueOperations.increment("pwd_reset_attempts:" + token)).thenReturn(1L);
        when(userRepository.findByEmail("user@b.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.resetPassword(token, "weak"));

        verify(valueOperations).increment("pwd_reset_attempts:" + token);
    }
}
