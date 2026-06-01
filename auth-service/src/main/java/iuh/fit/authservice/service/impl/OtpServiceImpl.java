package iuh.fit.authservice.service.impl;

import iuh.fit.authservice.entity.OtpType;
import iuh.fit.authservice.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    private String otpKey(String email, OtpType type) {
        return "otp:" + type.name() + ":" + email.toLowerCase().trim();
    }

    private String attemptsKey(String email, OtpType type) {
        return "otp:attempts:" + type.name() + ":" + email.toLowerCase().trim();
    }

    private String cooldownKey(String email, OtpType type) {
        return "otp:cooldown:" + type.name() + ":" + email.toLowerCase().trim();
    }

    private String generateOtp() {
        int length = Math.max(4, Math.min(otpLength, 10));
        int bound = (int) Math.pow(10, length);
        int min = bound / 10;
        int code = secureRandom.nextInt(bound - min) + min;
        return String.valueOf(code);
    }

    @Override
    public int getExpiryMinutes() {
        return expiryMinutes;
    }

    @Override
    public void assertResendAllowed(String email, OtpType type) {
        String normalized = email.toLowerCase().trim();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey(normalized, type)))) {
            throw new IllegalArgumentException(
                    "Vui lòng đợi " + resendCooldownSeconds + " giây trước khi gửi lại mã OTP"
            );
        }
    }

    @Override
    public void markResendCooldown(String email, OtpType type) {
        String normalized = email.toLowerCase().trim();
        redisTemplate.opsForValue().set(
                cooldownKey(normalized, type),
                "1",
                resendCooldownSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public String sendOtp(String email, OtpType type) {
        String normalized = email.toLowerCase().trim();
        assertResendAllowed(normalized, type);

        String otp = generateOtp();
        redisTemplate.opsForValue().set(
                otpKey(normalized, type),
                otp,
                expiryMinutes,
                TimeUnit.MINUTES
        );
        redisTemplate.delete(attemptsKey(normalized, type));
        markResendCooldown(normalized, type);
        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp, OtpType type) {
        String normalized = email.toLowerCase().trim();
        String key = otpKey(normalized, type);
        Object storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            return false;
        }

        if (storedOtp.toString().equals(otp)) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptsKey(normalized, type));
            return true;
        }

        String attemptsRedisKey = attemptsKey(normalized, type);
        Long attempts = redisTemplate.opsForValue().increment(attemptsRedisKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsRedisKey, expiryMinutes, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts >= maxAttempts) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptsRedisKey);
            throw new IllegalArgumentException("Quá nhiều lần nhập sai OTP. Vui lòng yêu cầu mã mới");
        }
        return false;
    }
}
