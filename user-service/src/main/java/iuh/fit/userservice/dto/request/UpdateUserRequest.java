package iuh.fit.userservice.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String displayName;
    private String bio;
    private String phone;
    /** ISO-8601 date: yyyy-MM-dd */
    private String dateOfBirth;
    private String gender;
    private String locale;
    private Boolean online;
}
