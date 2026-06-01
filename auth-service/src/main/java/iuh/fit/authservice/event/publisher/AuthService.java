package iuh.fit.authservice.event.publisher;

import iuh.fit.authservice.config.MediaProperties;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.entity.OtpType;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.LoginOtpEvent;
import iuh.fit.authservice.event.payload.UserRegisteredEvent;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import iuh.fit.authservice.service.OtpService;
import iuh.fit.authservice.service.PendingRegistrationService;
import iuh.fit.authservice.util.PasswordPolicyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final RabbitTemplate rabbitTemplate;
    private final MediaProperties mediaProperties;
    private final OtpService otpService;
    private final PendingRegistrationService pendingRegistrationService;
    private final PasswordPolicyValidator passwordPolicyValidator;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    @Value("${rabbitmq.login-otp.routing-key:auth.login-otp}")
    private String loginOtpRoutingKey;

    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        passwordPolicyValidator.validate(request.getPassword());

        pendingRegistrationService.save(request);

        String otp = otpService.sendOtp(email, OtpType.REGISTER);
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                new UserRegisteredEvent(
                        email,
                        request.getUsername(),
                        otp,
                        otpService.getExpiryMinutes()
                )
        );
    }

    public void resendRegistrationOtp(String email) {
        String normalized = normalizeEmail(email);
        if (!pendingRegistrationService.hasPending(normalized)) {
            throw new IllegalStateException("Phiên đăng ký đã hết hạn, vui lòng đăng ký lại");
        }

        RegisterRequest pending = pendingRegistrationService.get(normalized);
        String otp = otpService.sendOtp(normalized, OtpType.REGISTER);
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                new UserRegisteredEvent(
                        normalized,
                        pending.getUsername(),
                        otp,
                        otpService.getExpiryMinutes()
                )
        );
    }

    @Transactional
    public User verifyRegistrationOtp(String email, String otp) {
        String normalized = normalizeEmail(email);
        boolean valid = otpService.verifyOtp(normalized, otp, OtpType.REGISTER);
        if (!valid) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        RegisterRequest pending = pendingRegistrationService.get(normalized);

        if (userRepository.existsByEmail(normalized)) {
            pendingRegistrationService.delete(normalized);
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByUsername(pending.getUsername())) {
            pendingRegistrationService.delete(normalized);
            throw new UserAlreadyExistsException("Username already exists");
        }

        User user = new User(
                pending.getEmail(),
                pending.getUsername(),
                encoder.encode(pending.getPassword())
        );
        user.setEmailVerified(true);
        user.applyDefaultAvatar(
                mediaProperties.defaultAvatarUrl(),
                MediaProperties.DEFAULT_AVATAR_KEY
        );
        userRepository.save(user);
        pendingRegistrationService.delete(normalized);
        return user;
    }

    public void sendLoginOtp(String email) {
        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không hợp lệ"));

        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không hợp lệ");
        }

        String otp = otpService.sendOtp(normalized, OtpType.LOGIN);
        rabbitTemplate.convertAndSend(
                exchange,
                loginOtpRoutingKey,
                new LoginOtpEvent(normalized, otp, otpService.getExpiryMinutes())
        );
    }

    public void resendLoginOtp(String email) {
        sendLoginOtp(email);
    }

    public User verifyLoginOtp(String email, String otp) {
        String normalized = normalizeEmail(email);
        boolean valid = otpService.verifyOtp(normalized, otp, OtpType.LOGIN);
        if (!valid) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không hợp lệ"));

        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không hợp lệ");
        }

        return user;
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        passwordPolicyValidator.validate(newPassword);

        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        if (encoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        user.setPasswordHash(encoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
