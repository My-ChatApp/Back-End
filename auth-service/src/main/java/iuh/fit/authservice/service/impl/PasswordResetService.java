package iuh.fit.authservice.service.impl;

import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.PasswordResetEvent;
import iuh.fit.authservice.repository.UserRepository;
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

    /**
     * Gửi link reset password cho email
     */
    public void sendPasswordResetEmail(String email) {
        log.info("[PasswordResetService] Processing forgot password for email: {}", email);

        // 1. Kiểm tra email có tồn tại không
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Không trả về lỗi để tránh lộ email (security best practice)
            log.warn("[PasswordResetService] Reset password requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // 2. Tạo secure token
        String resetToken = generateSecureToken();

        // 3. Lưu token vào Redis với TTL
        String redisKey = REDIS_KEY_PREFIX + resetToken;
        redisTemplate.opsForValue().set(redisKey, email, tokenExpiryMinutes, TimeUnit.MINUTES);
        log.info("[PasswordResetService] Reset token saved to Redis with TTL {} minutes", tokenExpiryMinutes);

        // 4. Tạo reset link
        String resetLink = resetLinkBase + "?token=" + resetToken;

        // 5. Publish event để gửi email
        PasswordResetEvent event = new PasswordResetEvent(
                email,
                resetToken,
                resetLink,
                tokenExpiryMinutes
        );

        rabbitTemplate.convertAndSend(exchange, passwordResetRoutingKey, event);
        log.info("[PasswordResetService] Password reset event published for email: {}", email);
    }

    /**
     * Xác thực và reset mật khẩu
     */
    public void resetPassword(String token, String newPassword) {
        log.info("[PasswordResetService] Processing password reset with token");

        // 1. Tra Redis bằng token
        String redisKey = REDIS_KEY_PREFIX + token;
        String email = redisTemplate.opsForValue().get(redisKey);

        if (email == null) {
            log.warn("[PasswordResetService] Invalid or expired reset token");
            throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
        }

        // 2. Kiểm tra số lần thử
        String attemptsKey = REDIS_ATTEMPTS_PREFIX + token;
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= maxAttempts) {
            log.warn("[PasswordResetService] Max reset attempts exceeded for token");
            redisTemplate.delete(redisKey);
            throw new IllegalArgumentException("Quá nhiều lần thử. Vui lòng yêu cầu link reset mới");
        }

        // 3. Lấy user từ database
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.error("[PasswordResetService] User not found for email: {}", email);
            throw new IllegalArgumentException("Người dùng không tồn tại");
        }

        User user = userOpt.get();

        // 4. Validate mật khẩu mới
        validateNewPassword(newPassword, user);

        // 5. Hash và lưu mật khẩu mới
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
        log.info("[PasswordResetService] Password reset successfully for user: {}", email);

        // 6. Xoá token từ Redis
        redisTemplate.delete(redisKey);
        redisTemplate.delete(attemptsKey);
        log.info("[PasswordResetService] Reset token deleted from Redis");

        // 7. (Optional) Publish event để gửi mail xác nhận
        publishPasswordResetConfirmationEvent(email);
    }

    /**
     * Xác thực token có hợp lệ không
     */
    public boolean validateResetToken(String token) {
        String redisKey = REDIS_KEY_PREFIX + token;
        String email = redisTemplate.opsForValue().get(redisKey);
        return email != null;
    }

    /**
     * Sinh secure token (32 bytes -> Base64 URL-safe)
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Validate mật khẩu mới
     */
    private void validateNewPassword(String newPassword, User user) {
        // Kiểm tra độ dài
        if (newPassword.length() < 8 || newPassword.length() > 100) {
            throw new IllegalArgumentException("Mật khẩu phải từ 8 đến 100 ký tự");
        }

        // Kiểm tra password không giống password cũ
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        // Kiểm tra password có đủ độ phức tạp không
        if (!isPasswordComplex(newPassword)) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải chứa ít nhất: " +
                    "1 chữ hoa, 1 chữ thường, 1 chữ số, 1 ký tự đặc biệt"
            );
        }
    }

    /**
     * Kiểm tra độ phức tạp của password
     */
    private boolean isPasswordComplex(String password) {
        return password.matches(".*[A-Z].*") &&        // Có chữ hoa
               password.matches(".*[a-z].*") &&        // Có chữ thường
               password.matches(".*\\d.*") &&          // Có chữ số
               password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*"); // Có ký tự đặc biệt
    }

    /**
     * Publish event xác nhận reset password (optional)
     */
    private void publishPasswordResetConfirmationEvent(String email) {
        try {
            PasswordResetEvent confirmationEvent = new PasswordResetEvent(
                    email,
                    null,
                    null,
                    0
            );
            // Publish với routing key khác để notification service phân biệt
            rabbitTemplate.convertAndSend(
                    exchange,
                    "auth.password-reset-confirmed",
                    confirmationEvent
            );
            log.info("[PasswordResetService] Password reset confirmation event published");
        } catch (Exception e) {
            log.warn("[PasswordResetService] Failed to publish confirmation event: {}", e.getMessage());
            // Không throw exception vì password đã reset thành công
        }
    }
}
