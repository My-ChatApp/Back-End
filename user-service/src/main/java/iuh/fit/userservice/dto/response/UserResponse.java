package iuh.fit.userservice.dto.response;

import iuh.fit.userservice.entity.User;
import lombok.Builder;
import lombok.Data;

import iuh.fit.userservice.entity.Gender;

import java.time.Instant;
import java.time.LocalDate;
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
    private LocalDate dateOfBirth;
    private Gender gender;
    private String locale;
    private boolean online;
    private Instant lastSeenAt;
    private boolean active;
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
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .locale(user.getLocale())
                .online(user.isOnline())
                .lastSeenAt(user.getLastSeenAt())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
