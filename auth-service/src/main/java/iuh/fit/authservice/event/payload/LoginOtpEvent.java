package iuh.fit.authservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginOtpEvent {
    private String email;
    private String otp;
    private int otpExpiryMinutes;
}
