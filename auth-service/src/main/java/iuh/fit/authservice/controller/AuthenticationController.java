package iuh.fit.authservice.controller;

import iuh.fit.authservice.dto.request.LoginRequest;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.dto.response.AuthResponse;
import iuh.fit.authservice.dto.response.LoginResponse;
import iuh.fit.authservice.event.publisher.AuthService;
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

    @Autowired
    public AuthenticationController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtils,
            AuthService authService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.authService = authService;
    }

    @PostMapping("/signin")
    public ApiResponse<LoginResponse> authenticateUser(@RequestBody LoginRequest user) {
        log.info("[AuthenticationController] Login attempt for email: {}", user.getEmail());
        return issueTokenForCredentials(user.getEmail(), user.getPassword(), "Login successful");
    }

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> registerUser(@RequestBody RegisterRequest request) {
        authService.register(request);
        log.info("[AuthenticationController] User registered, issuing token for: {}", request.getEmail());
        return issueTokenForCredentials(
                request.getEmail(),
                request.getPassword(),
                "User registered successfully"
        );
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
}