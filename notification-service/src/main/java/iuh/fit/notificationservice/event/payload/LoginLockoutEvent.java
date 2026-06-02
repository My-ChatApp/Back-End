package iuh.fit.notificationservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLockoutEvent {
    private String userId;
    private String email;
    private String clientIp;
    private Instant lockedUntil;
    private int lockoutMinutes;
}
