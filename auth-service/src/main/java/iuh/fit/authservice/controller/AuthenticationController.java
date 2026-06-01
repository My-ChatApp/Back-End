package iuh.fit.authservice.controller;

import iuh.fit.authservice.dto.request.ForgotPasswordRequest;
import iuh.fit.authservice.dto.request.LoginRequest;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.dto.request.ResetPasswordRequest;
import iuh.fit.authservice.dto.request.VerifyOtpRequest;
import iuh.fit.authservice.dto.response.AuthResponse;
import iuh.fit.authservice.dto.response.LoginResponse;
import iuh.fit.authservice.dto.response.PasswordResetResponse;
import iuh.fit.authservice.event.publisher.AuthService;
import iuh.fit.authservice.service.impl.PasswordResetService;
import iuh.fit.authservice.util.JwtUtil;
import iuh.fit.common.dto.response.ApiResponse;
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

    @Autowired
    public AuthenticationController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtils,
            AuthService authService,
            PasswordResetService passwordResetService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/signin")
    public ApiResponse<LoginResponse> authenticateUser(@RequestBody LoginRequest user) {
        log.info("[AuthenticationController] Login attempt for email: {}", user.getEmail());
        return issueTokenForCredentials(user.getEmail(), user.getPassword(), "Login successful");
    }

    @PostMapping("/sigin-otp")
    public ApiResponse<LoginResponse> authenticateUserWithOtp(@RequestBody VerifyOtpRequest request) {
        log.info("[AuthenticationController] OTP login attempt for email: {}", request.getEmail());
        // 1. Verify OTP
        authService.verifyOtp(request.getEmail(), request.getOtp());
        // 2. Issue token if OTP is valid
        return issueTokenForCredentials(request.getEmail(), "OTP_VERIFIED", "Login successful with OTP");
    }

    @PostMapping("/signup")
    public ApiResponse<Void> registerUser(@RequestBody RegisterRequest request) {
        authService.register(request);
        log.info("[AuthenticationController] User registered, issuing token for: {}", request.getEmail());
        return new ApiResponse<>(true, "Registration successful, please verify OTP sent to your email", null);
    }

    private ApiResponse<LoginResponse> issueTokenForCredentials(
            String email,
            String password,
            String successMessage
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            log.info("[AuthenticationController] Authentication successful for: {}", email);

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
            return new ApiResponse<>(false, "Invalid email or password", null);
        }
    }

    @PostMapping("/validate")
    public AuthResponse validateToken(@RequestHeader ("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        boolean isValid = jwtUtils.validateJwtToken(jwt);
        String email = isValid ? jwtUtils.getUserFromToken(jwt) : null;
        return new AuthResponse(isValid, email);
    }

    // AuthController
    @PostMapping("/verify-otp")
    public ApiResponse<Void> verifyOtp(@RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getEmail(), request.getOtp());
        return new ApiResponse<>(true, "OTP verified successfully", null);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestHeader("Authorization") String token, @RequestBody String newPassword) {
        String jwt = token.replace("Bearer ", "");
        if (!passwordResetService.validateResetToken(token)) {
            return new ApiResponse<>(false, "Invalid token", null);
        }
        String email = jwtUtils.getUserFromToken(jwt);
        authService.changePassword(email, newPassword);
        return new ApiResponse<>(true, "Password changed successfully", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<PasswordResetResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        log.info("[AuthenticationController] Forgot password request for email: {}", request.getEmail());
        try {
            passwordResetService.sendPasswordResetEmail(request.getEmail());
            // Always return success message (even if email doesn't exist) to avoid email enumeration
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
    public ApiResponse<PasswordResetResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
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