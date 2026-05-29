package iuh.fit.userservice.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private MultipartFile avatar;
    /** Sau presign upload (media-service) — URL CDN */
    private String avatarUrl;
    /** S3 object key từ presign response */
    private String avatarS3Key;
    private String phone;
    /** ISO-8601 date: yyyy-MM-dd */
    private String dateOfBirth;
    /** MALE | FEMALE | OTHER — empty string clears */
    private String gender;
}
