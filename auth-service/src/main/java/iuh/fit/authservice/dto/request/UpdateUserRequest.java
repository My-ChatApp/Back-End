package iuh.fit.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Email
    private String email;

    @Size(max = 50)
    private String username;

    @Size(max = 100)
    private String displayName;

    @Size(max = 500)
    private String bio;

    @Size(max = 20)
    private String phone;

    @Size(max = 10)
    private String locale;

    private Boolean emailVerified;
    private Boolean online;
    private Boolean active;
}
