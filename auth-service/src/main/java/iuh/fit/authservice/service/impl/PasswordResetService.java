package iuh.fit.authservice.service.impl;

import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.PasswordResetEvent;
import iuh.fit.authservice.repository.UserRepository;
import iuh.fit.authservice.util.PasswordPolicyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordPolicyValidator passwordPolicyValidator;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.password-reset.routing-key:auth.password-reset}")
    private String passwordResetRoutingKey;

    @Value("${password-reset.token-expiry-minutes:15}")
    private int tokenExpiryMinutes;

    @Value("${password-reset.reset-link:http://localhost:3000/reset-password}")
    private String resetLinkBase;

    @Value("${password-reset.max-attempts:3}")
    private int maxAttempts;

    private static final String REDIS_KEY_PREFIX = "pwd_reset:";
    private static final String REDIS_ATTEMPTS_PREFIX = "pwd_reset_attempts:";

    public void sendPasswordResetEmail(String email) {
        log.info("[PasswordResetService] Processing forgot password for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            log.warn("[PasswordResetService] Reset password requested for non-existent email: {}", email);
            return;
        }

        String resetToken = generateSecureToken();
        String redisKey = REDIS_KEY_PREFIX + resetToken;
        redisTemplate.opsForValue().set(redisKey, userOpt.get().getEmail(), tokenExpiryMinutes, TimeUnit.MINUTES);
        log.info("[PasswordResetService] Reset token saved to Redis with TTL {} minutes", tokenExpiryMinutes);

        String resetLink = resetLinkBase + "?token=" + resetToken;

        PasswordResetEvent event = new PasswordResetEvent(
                userOpt.get().getEmail(),
                resetToken,
                resetLink,
                tokenExpiryMinutes
        );

        rabbitTemplate.convertAndSend(exchange, passwordResetRoutingKey, event);
        log.info("[PasswordResetService] Password reset event published for email: {}", email);
    }

    public void resetPassword(String token, String newPassword) {
        log.info("[PasswordResetService] Processing password reset with token");

        String redisKey = REDIS_KEY_PREFIX + token;
        String email = redisTemplate.opsForValue().get(redisKey);

        if (email == null) {
            log.warn("[PasswordResetService] Invalid or expired reset token");
            throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
        }

        String attemptsKey = REDIS_ATTEMPTS_PREFIX + token;
        if (isAttemptsExceeded(attemptsKey, redisKey)) {
            throw new IllegalArgumentException("Quá nhiều lần thử. Vui lòng yêu cầu link reset mới");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.error("[PasswordResetService] User not found for email: {}", email);
            throw new IllegalArgumentException("Người dùng không tồn tại");
        }

        User user = userOpt.get();

        try {
            validateNewPassword(newPassword, user);
        } catch (IllegalArgumentException ex) {
            incrementAttempts(attemptsKey);
            if (isAttemptsExceeded(attemptsKey, redisKey)) {
                throw new IllegalArgumentException("Quá nhiều lần thử. Vui lòng yêu cầu link reset mới");
            }
            throw ex;
        }

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
        log.info("[PasswordResetService] Password reset successfully for user: {}", email);

        redisTemplate.delete(redisKey);
        redisTemplate.delete(attemptsKey);
        log.info("[PasswordResetService] Reset token deleted from Redis");

        publishPasswordResetConfirmationEvent(email);
    }

    public boolean validateResetToken(String token) {
        String redisKey = REDIS_KEY_PREFIX + token;
        String email = redisTemplate.opsForValue().get(redisKey);
        return email != null;
    }

    private void incrementAttempts(String attemptsKey) {
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsKey, tokenExpiryMinutes, TimeUnit.MINUTES);
        }
    }

    private boolean isAttemptsExceeded(String attemptsKey, String redisKey) {
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= maxAttempts) {
            log.warn("[PasswordResetService] Max reset attempts exceeded for token");
            redisTemplate.delete(redisKey);
            redisTemplate.delete(attemptsKey);
            return true;
        }
        return false;
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private void validateNewPassword(String newPassword, User user) {
        passwordPolicyValidator.validate(newPassword);

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ");
        }
    }

    private void publishPasswordResetConfirmationEvent(String email) {
        try {
            PasswordResetEvent confirmationEvent = new PasswordResetEvent(
                    email,
                    null,
                    null,
                    0
            );
            rabbitTemplate.convertAndSend(
                    exchange,
                    "auth.password-reset-confirmed",
                    confirmationEvent
            );
            log.info("[PasswordResetService] Password reset confirmation event published");
        } catch (Exception e) {
            log.warn("[PasswordResetService] Failed to publish confirmation event: {}", e.getMessage());
        }
    }
}
