package iuh.fit.authservice.event.publisher;

import iuh.fit.authservice.config.MediaProperties;
import iuh.fit.authservice.dto.request.ChangePasswordRequest;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.payload.UserRegisteredEvent;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import iuh.fit.authservice.service.OtpService;
import iuh.fit.authservice.service.PendingRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }


        pendingRegistrationService.save(request);

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                new UserRegisteredEvent(
                        request.getEmail(),
                        request.getUsername(),
                        otpService.sendOtp(request.getEmail()),
                        5
                )
        );
    }

    public void changePassword(String email, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải ít nhất 8 ký tự");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        user.setPasswordHash(encoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
    }

    public void verifyOtp(String email, String otp) {
        // 1. Check OTP — tự xoá sau khi verify thành công
        boolean valid = otpService.verifyOtp(email, otp);
        if (!valid) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        // 2. Lấy thông tin đăng ký tạm từ Redis
        RegisterRequest pending = pendingRegistrationService.get(email);

        // 3. Save User vào DB
        User user = new User(
                pending.getEmail(),
                pending.getUsername(),
                encoder.encode(pending.getPassword())
        );
        user.applyDefaultAvatar(
                mediaProperties.defaultAvatarUrl(),
                MediaProperties.DEFAULT_AVATAR_KEY
        );
        userRepository.save(user);

        // 4. Xoá pending khỏi Redis
        pendingRegistrationService.delete(email);
    }
}
