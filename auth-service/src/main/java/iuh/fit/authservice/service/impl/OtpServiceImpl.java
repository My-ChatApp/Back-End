package iuh.fit.authservice.service.impl;

import iuh.fit.authservice.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long OTP_EXPIRE_MINUTES = 5;

    private String buildKey(String email) {
        return "otp:" + email;
    }

    private String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    @Override
    public String sendOtp(String email) {
        String otp = generateOtp();

        // lưu Redis
        redisTemplate.opsForValue().set(
                buildKey(email),
                otp,
                OTP_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        String key = buildKey(email);

        Object storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) return false;

        if (storedOtp.toString().equals(otp)) {
            redisTemplate.delete(key); // dùng 1 lần
            return true;
        }

        return false;
    }
}