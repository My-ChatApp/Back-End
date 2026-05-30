package iuh.fit.authservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class HealthController {

    @GetMapping("/health")
    public String health() {
        log.info("[HealthController] Health check called");
        return "Auth Service is running!";
    }
}
