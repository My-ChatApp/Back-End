package iuh.fit.authservice.controller;

import iuh.fit.authservice.dto.request.ChangePasswordRequest;
import iuh.fit.authservice.dto.request.EmailRequest;
import iuh.fit.authservice.dto.request.ForgotPasswordRequest;
import iuh.fit.authservice.dto.request.LoginRequest;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.dto.request.ResetPasswordRequest;
import iuh.fit.authservice.dto.request.VerifyOtpRequest;
import iuh.fit.authservice.dto.response.AuthResponse;
import iuh.fit.authservice.dto.response.LoginResponse;
import iuh.fit.authservice.dto.response.PasswordResetResponse;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.publisher.AuthService;
import iuh.fit.authservice.event.publisher.ChangePasswordService;
import iuh.fit.authservice.service.CustomUserDetailsService;
import iuh.fit.authservice.service.LoginLockoutService;
import iuh.fit.authservice.service.impl.PasswordResetService;
import iuh.fit.authservice.util.ClientIpResolver;
import iuh.fit.authservice.util.JwtUtil;
import iuh.fit.common.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtils;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final CustomUserDetailsService userDetailsService;
    private final ChangePasswordService changePasswordService;
    private final LoginLockoutService loginLockoutService;

    @Autowired
    public AuthenticationController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtils,
            AuthService authService,
            PasswordResetService passwordResetService,
            CustomUserDetailsService userDetailsService,
            ChangePasswordService changePasswordService,
            LoginLockoutService loginLockoutService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.userDetailsService = userDetailsService;
        this.changePasswordService = changePasswordService;
        this.loginLockoutService = loginLockoutService;
    }

    @PostMapping("/signin")
    public ApiResponse<LoginResponse> authenticateUser(
            @Valid @RequestBody LoginRequest user,
            HttpServletRequest request
    ) {
        log.info("[AuthenticationController] Login attempt for email: {}", user.getEmail());
        return issueTokenForCredentials(
                user.getEmail(),
                user.getPassword(),
                "Login successful",
                ClientIpResolver.resolve(request)
        );
    }

    @PostMapping("/signup")
    public ApiResponse<Void> registerUser(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        log.info("[AuthenticationController] Signup pending OTP for: {}", request.getEmail());
        return new ApiResponse<>(true, "Registration successful, please verify OTP sent to your email", null);
    }

    @PostMapping("/signup/resend-otp")
    public ApiResponse<Void> resendRegistrationOtp(@Valid @RequestBody EmailRequest request) {
        authService.resendRegistrationOtp(request.getEmail());
        return new ApiResponse<>(true, "OTP đã được gửi lại tới email của bạn", null);
    }

    @PostMapping("/verify-otp")
    public ApiResponse<LoginResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        User user = authService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return new ApiResponse<>(true, "OTP verified successfully", issueTokenForUser(user));
    }

    @PostMapping("/signin-otp/send")
    public ApiResponse<Void> sendLoginOtp(@Valid @RequestBody EmailRequest request) {
        authService.sendLoginOtp(request.getEmail());
        return new ApiResponse<>(true, "Mã OTP đăng nhập đã được gửi tới email của bạn", null);
    }

    @PostMapping("/signin-otp/resend")
    public ApiResponse<Void> resendLoginOtp(@Valid @RequestBody EmailRequest request) {
        authService.resendLoginOtp(request.getEmail());
        return new ApiResponse<>(true, "Mã OTP đăng nhập đã được gửi lại", null);
    }

    @PostMapping("/signin-otp")
    public ApiResponse<LoginResponse> loginWithOtp(@Valid @RequestBody VerifyOtpRequest request) {
        User user = authService.verifyLoginOtp(request.getEmail(), request.getOtp());
        return new ApiResponse<>(true, "Login successful with OTP", issueTokenForUser(user));
    }

    private ApiResponse<LoginResponse> issueTokenForCredentials(
            String email,
            String password,
            String successMessage,
            String clientIp
    ) {
        var lockout = loginLockoutService.getActiveLockout(clientIp);
        if (lockout.isPresent()) {
            String message = loginLockoutService.formatLockoutMessage(lockout.get().retryAfterSeconds());
            log.warn("[AuthenticationController] Blocked sign-in from IP {} for email {}", clientIp, email);
            return new ApiResponse<>(false, message, null);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            log.info("[AuthenticationController] Authentication successful for: {}", email);
            loginLockoutService.clearFailedAttempts(clientIp);

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            if (userDetails == null) {
                log.error("[AuthenticationController] UserDetails is null after authentication");
                return new ApiResponse<>(false, "Invalid email or password", null);
            }
            String token = jwtUtils.generateToken(userDetails);
            log.info("[AuthenticationController] Token generated for: {}", email);
            return new ApiResponse<>(true, successMessage, new LoginResponse(token));
        } catch (Exception e) {
            log.error("[AuthenticationController] Authentication failed for {}: {}", email, e.getMessage());
            loginLockoutService.recordFailedAttempt(clientIp, email);
            var afterFailure = loginLockoutService.getActiveLockout(clientIp);
            if (afterFailure.isPresent()) {
                return new ApiResponse<>(
                        false,
                        loginLockoutService.formatLockoutMessage(afterFailure.get().retryAfterSeconds()),
                        null
                );
            }
            return new ApiResponse<>(false, "Invalid email or password", null);
        }
    }

    private LoginResponse issueTokenForUser(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtils.generateToken(userDetails);
        return new LoginResponse(token);
    }

    @PostMapping("/validate")
    public AuthResponse validateToken(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        boolean isValid = jwtUtils.validateJwtToken(jwt);
        String email = isValid ? jwtUtils.getUserFromToken(jwt) : null;
        return new AuthResponse(isValid, email);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String jwt = token.replace("Bearer ", "");
        if (!jwtUtils.validateJwtToken(jwt)) {
            return new ApiResponse<>(false, "Invalid token", null);
        }
        String email = jwtUtils.getUserFromToken(jwt);
        authService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        changePasswordService.publishChangePasswordEvent(email);
        return new ApiResponse<>(true, "Password changed successfully", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("[AuthenticationController] Forgot password request for email: {}", request.getEmail());
        try {
            passwordResetService.sendPasswordResetEmail(request.getEmail());
            return new ApiResponse<>(
                    true,
                    "Nếu email tồn tại, link reset password sẽ được gửi trong vài phút",
                    new PasswordResetResponse("Check your email for password reset link")
            );
        } catch (Exception e) {
            log.error("[AuthenticationController] Error processing forgot password: {}", e.getMessage());
            return new ApiResponse<>(
                    true,
                    "Nếu email tồn tại, link reset password sẽ được gửi trong vài phút",
                    new PasswordResetResponse("Check your email for password reset link")
            );
        }
    }

    @PostMapping("/reset-password")
    public ApiResponse<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("[AuthenticationController] Password reset request received");
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return new ApiResponse<>(
                    true,
                    "Mật khẩu đã được đặt lại thành công",
                    new PasswordResetResponse("Password reset successfully")
            );
        } catch (IllegalArgumentException e) {
            log.warn("[AuthenticationController] Invalid reset attempt: {}", e.getMessage());
            return new ApiResponse<>(false, e.getMessage(), null);
        } catch (Exception e) {
            log.error("[AuthenticationController] Error resetting password: {}", e.getMessage());
            return new ApiResponse<>(false, "Đặt lại mật khẩu thất bại. Vui lòng thử lại", null);
        }
    }

    @GetMapping("/validate-reset-token")
    public ApiResponse<Boolean> validateResetToken(@RequestParam String token) {
        log.info("[AuthenticationController] Validating reset token");
        try {
            boolean isValid = passwordResetService.validateResetToken(token);
            if (isValid) {
                return new ApiResponse<>(true, "Token hợp lệ", true);
            } else {
                return new ApiResponse<>(false, "Token không hợp lệ hoặc đã hết hạn", false);
            }
        } catch (Exception e) {
            log.error("[AuthenticationController] Error validating token: {}", e.getMessage());
            return new ApiResponse<>(false, "Lỗi khi xác thực token", false);
        }
    }
}
