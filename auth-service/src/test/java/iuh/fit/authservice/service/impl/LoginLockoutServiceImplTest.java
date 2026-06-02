package iuh.fit.authservice.service.impl;

import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.LoginLockoutEvent;
import iuh.fit.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginLockoutServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private LoginLockoutServiceImpl loginLockoutService;

    @BeforeEach
    void setUp() {
        loginLockoutService = new LoginLockoutServiceImpl(redisTemplate, userRepository, rabbitTemplate);
        ReflectionTestUtils.setField(loginLockoutService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(loginLockoutService, "lockoutMinutes", 5);
        ReflectionTestUtils.setField(loginLockoutService, "failWindowMinutes", 15);
        ReflectionTestUtils.setField(loginLockoutService, "exchange", "auth.exchange");
        ReflectionTestUtils.setField(loginLockoutService, "loginLockoutRoutingKey", "auth.login-lockout");
    }

    @Test
    void getActiveLockout_whenBlocked_returnsRetryAfter() {
        when(redisTemplate.hasKey("login:block:ip:1.2.3.4")).thenReturn(true);
        when(redisTemplate.getExpire("login:block:ip:1.2.3.4", TimeUnit.SECONDS)).thenReturn(120L);

        var status = loginLockoutService.getActiveLockout("1.2.3.4");

        assertTrue(status.isPresent());
        assertEquals(120L, status.get().retryAfterSeconds());
    }

    @Test
    void recordFailedAttempt_fifthFailure_locksIpAndPublishesEvent() {
        when(redisTemplate.hasKey("login:block:ip:10.0.0.1")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("login:fail:ip:10.0.0.1")).thenReturn(5L);

        User user = new User("user@b.com", "user", "hash");
        user.setId(UUID.randomUUID());
        user.setActive(true);
        when(userRepository.findByEmail("user@b.com")).thenReturn(Optional.of(user));

        boolean locked = loginLockoutService.recordFailedAttempt("10.0.0.1", "user@b.com");

        assertTrue(locked);
        verify(redisTemplate).delete("login:fail:ip:10.0.0.1");
        verify(valueOperations).set("login:block:ip:10.0.0.1", "1", 5L, TimeUnit.MINUTES);

        ArgumentCaptor<LoginLockoutEvent> eventCaptor = ArgumentCaptor.forClass(LoginLockoutEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("auth.exchange"), eq("auth.login-lockout"), eventCaptor.capture());
        assertEquals(user.getId().toString(), eventCaptor.getValue().getUserId());
    }

    @Test
    void recordFailedAttempt_belowThreshold_doesNotLock() {
        when(redisTemplate.hasKey("login:block:ip:10.0.0.2")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("login:fail:ip:10.0.0.2")).thenReturn(2L);

        boolean locked = loginLockoutService.recordFailedAttempt("10.0.0.2", "user@b.com");

        assertFalse(locked);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(LoginLockoutEvent.class));
    }

    @Test
    void clearFailedAttempts_deletesCounter() {
        loginLockoutService.clearFailedAttempts("10.0.0.3");
        verify(redisTemplate).delete("login:fail:ip:10.0.0.3");
    }
}
