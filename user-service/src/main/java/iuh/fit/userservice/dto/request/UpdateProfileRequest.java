package iuh.fit.userservice.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private MultipartFile avatar;
}
