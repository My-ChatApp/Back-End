package iuh.fit.notificationservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetEvent {
    private String email;
    private String resetToken;
    private String resetLink;
    private int expiryMinutes;
}
