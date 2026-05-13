package iuh.fit.authservice.controller;

import iuh.fit.authservice.dto.request.LoginRequest;
import iuh.fit.authservice.dto.request.RegisterRequest;
import iuh.fit.authservice.dto.response.AuthResponse;
import iuh.fit.authservice.dto.response.LoginResponse;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.event.publisher.AuthService;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import iuh.fit.authservice.util.JwtUtil;
import iuh.fit.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthenticationController {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private PasswordEncoder encoder;
    private JwtUtil jwtUtils;
    @Autowired
    private AuthService authService;

    @Autowired
    public AuthenticationController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder encoder,
            JwtUtil jwtUtils
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }


    @PostMapping("/signin")
    public ApiResponse<LoginResponse> authenticateUser(@RequestBody LoginRequest user) {
        log.info("[AuthenticationController] Login attempt for email: {}", user.getEmail());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            user.getPassword()
                    )
            );

            log.info("[AuthenticationController] Authentication successful for: {}", user.getEmail());

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            if(userDetails == null) {
                log.error("[AuthenticationController] UserDetails is null after authentication");
                return new ApiResponse<>( false, "Invalid email or password", null);
            }
            String token = jwtUtils.generateToken(userDetails);
            log.info("[AuthenticationController] Token generated for: {}", user.getEmail());
            return new ApiResponse<>( true, "Login successful", new LoginResponse(token));
        } catch (Exception e) {
            log.error("[AuthenticationController] Authentication failed for {}: {}", user.getEmail(), e.getMessage());
            return new ApiResponse<>( false, "Invalid email or password", null);
        }
    }

    @PostMapping("/signup")
    public ApiResponse<?> registerUser(
            @RequestBody RegisterRequest request) {

        authService.register(request);

        return new ApiResponse<>(
                true,
                "User registered successfully",
                null
        );
    }

    @PostMapping("/validate")
    public AuthResponse validateToken(@RequestHeader ("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        boolean isValid = jwtUtils.validateJwtToken(jwt);
        String email = isValid ? jwtUtils.getUserFromToken(jwt) : null;
        return new AuthResponse(isValid, email);
    }
}