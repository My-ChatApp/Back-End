package iuh.fit.authservice.service;

import iuh.fit.authservice.entity.OtpType;

public interface OtpService {
    String sendOtp(String email, OtpType type);

    boolean verifyOtp(String email, String otp, OtpType type);

    void assertResendAllowed(String email, OtpType type);

    void markResendCooldown(String email, OtpType type);

    int getExpiryMinutes();
}
