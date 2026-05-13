package iuh.fit.userservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    @Id
    private String id;

    private String userId;
    private String displayName;
    private String avatarUrl;
    private boolean online;
    private Instant lastSeen;


    public UserProfile(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.online = false;
        this.lastSeen = Instant.now();
        this.avatarUrl = "https://res.cloudinary.com/dbnrumnhu/image/upload/v1778607443/default-avatar-icon-of-social-media-user-vector_cja3v0.jpg";
    }
}
