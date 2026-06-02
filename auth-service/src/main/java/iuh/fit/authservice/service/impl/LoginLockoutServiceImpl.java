package iuh.fit.authservice.service.impl;

import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.LoginLockoutEvent;
import iuh.fit.authservice.repository.UserRepository;
import iuh.fit.authservice.service.LoginLockoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginLockoutServiceImpl implements LoginLockoutService {

    private static final String FAIL_KEY_PREFIX = "login:fail:ip:";
    private static final String BLOCK_KEY_PREFIX = "login:block:ip:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${login-lockout.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${login-lockout.lockout-minutes:5}")
    private int lockoutMinutes;

    @Value("${login-lockout.fail-window-minutes:15}")
    private int failWindowMinutes;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.login-lockout.routing-key:auth.login-lockout}")
    private String loginLockoutRoutingKey;

    @Override
    public Optional<LockoutStatus> getActiveLockout(String clientIp) {
        String blockKey = blockKey(clientIp);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
            return Optional.empty();
        }
        long retryAfter = ttlSeconds(blockKey);
        if (retryAfter <= 0) {
            redisTemplate.delete(blockKey);
            return Optional.empty();
        }
        return Optional.of(new LockoutStatus(retryAfter));
    }

    @Override
    public boolean recordFailedAttempt(String clientIp, String email) {
        if (getActiveLockout(clientIp).isPresent()) {
            return false;
        }

        String failKey = failKey(clientIp);
        Long attempts = redisTemplate.opsForValue().increment(failKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(failKey, failWindowMinutes, TimeUnit.MINUTES);
        }

        if (attempts == null || attempts < maxFailedAttempts) {
            return false;
        }

        applyLockout(clientIp, email);
        return true;
    }

    @Override
    public void clearFailedAttempts(String clientIp) {
        redisTemplate.delete(failKey(clientIp));
    }

    @Override
    public String formatLockoutMessage(long retryAfterSeconds) {
        long minutes = Math.max(1, (retryAfterSeconds + 59) / 60);
        return "Quá nhiều lần đăng nhập thất bại. Vui lòng thử lại sau "
                + minutes
                + " phút.";
    }

    private void applyLockout(String clientIp, String email) {
        String failKey = failKey(clientIp);
        String blockKey = blockKey(clientIp);
        redisTemplate.delete(failKey);
        redisTemplate.opsForValue().set(blockKey, "1", lockoutMinutes, TimeUnit.MINUTES);

        log.warn("[LoginLockout] IP {} locked for {} minutes after failed sign-in attempts", clientIp, lockoutMinutes);

        String normalizedEmail = email == null ? null : email.toLowerCase().trim();
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            return;
        }

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> publishLockoutNotification(user, clientIp));
    }

    private void publishLockoutNotification(User user, String clientIp) {
        if (!user.isActive() || user.getDeletedAt() != null) {
            return;
        }

        Instant lockedUntil = Instant.now().plusSeconds(lockoutMinutes * 60L);
        LoginLockoutEvent event = new LoginLockoutEvent(
                user.getId().toString(),
                user.getEmail(),
                clientIp,
                lockedUntil,
                lockoutMinutes
        );
        rabbitTemplate.convertAndSend(exchange, loginLockoutRoutingKey, event);
        log.info("[LoginLockout] Published lockout notification for userId={}", user.getId());
    }

    private long ttlSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 0 : ttl;
    }

    private static String failKey(String clientIp) {
        return FAIL_KEY_PREFIX + sanitizeIp(clientIp);
    }

    private static String blockKey(String clientIp) {
        return BLOCK_KEY_PREFIX + sanitizeIp(clientIp);
    }

    private static String sanitizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim();
    }
}
