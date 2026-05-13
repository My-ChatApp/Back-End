package iuh.fit.mailservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisteredEvent {
    private String userId;
    private String email;
    private String username;
}