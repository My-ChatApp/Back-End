package iuh.fit.authservice.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MainController {

    @GetMapping("/welcome")
    public String allAccess() {
        return "Everyone access";
    }

    @GetMapping("/user")
    public String userAccess() {
        return "User Content with JWT";
    }

    @GetMapping("/special")
    public String specialAccess() {
        return "Special access with JWT";
    }

    @GetMapping("/admin")
    public String adminAccess(@RequestHeader("X-Email") String email) {
        return "Admin Board with JWT" + email;
    }
}

