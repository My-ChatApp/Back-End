package iuh.fit.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @Size(max = 100)
    private String displayName;

    @Size(max = 500)
    private String bio;

    @Size(max = 20)
    private String phone;

    @Size(max = 10)
    private String locale;
}
