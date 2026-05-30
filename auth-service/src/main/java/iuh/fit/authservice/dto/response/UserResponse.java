package iuh.fit.authservice.dto.response;

import iuh.fit.authservice.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String phone;
    private String locale;
    private boolean emailVerified;
    private boolean online;
    private Instant lastSeenAt;
    private Instant lastLoginAt;
    private boolean active;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phone(user.getPhone())
                .locale(user.getLocale())
                .emailVerified(user.isEmailVerified())
                .online(user.isOnline())
                .lastSeenAt(user.getLastSeenAt())
                .lastLoginAt(user.getLastLoginAt())
                .active(user.isActive())
                .deletedAt(user.getDeletedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
