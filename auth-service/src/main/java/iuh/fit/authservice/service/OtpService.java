package iuh.fit.authservice.service;

public interface OtpService {
    String sendOtp(String email);
    boolean verifyOtp(String email, String otp);
}