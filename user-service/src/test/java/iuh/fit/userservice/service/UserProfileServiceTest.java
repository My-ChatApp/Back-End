package iuh.fit.userservice.service;

import iuh.fit.userservice.config.MediaProperties;
import iuh.fit.userservice.dto.request.UpdateProfileRequest;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.exception.ProfileNotFound;
import iuh.fit.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3ObjectStorageService s3ObjectStorageService;

    @InjectMocks
    private UserProfileService userProfileService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void updateProfile_userNotFound_throwsProfileNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFound.class,
                () -> userProfileService.updateProfile(userId.toString(), new UpdateProfileRequest()));
    }

    @Test
    void updateProfile_displayNameOnly_savesWithoutUpload() {
        User user = user(userId, "Old Name", null, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");

        User result = userProfileService.updateProfile(userId.toString(), request);

        assertEquals("New Name", result.getDisplayName());
        verify(s3ObjectStorageService, never()).uploadAvatar(any(), any());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_multipartAvatar_uploadsAndUpdates() {
        User user = user(userId, "User", "https://cdn.example.com/old.jpg", "users/old.jpg");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MultipartFile avatar = mock(MultipartFile.class);
        when(avatar.isEmpty()).thenReturn(false);
        when(s3ObjectStorageService.uploadAvatar(eq(userId.toString()), eq(avatar)))
                .thenReturn(new S3ObjectStorageService.UploadResult(
                        "https://cdn.example.com/users/new.jpg", "users/new.jpg"));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setAvatar(avatar);

        User result = userProfileService.updateProfile(userId.toString(), request);

        assertEquals("https://cdn.example.com/users/new.jpg", result.getAvatarUrl());
        assertEquals("users/new.jpg", result.getAvatarPublicId());
        verify(s3ObjectStorageService).deleteIfExists("users/old.jpg");
        verify(s3ObjectStorageService).uploadAvatar(userId.toString(), avatar);
    }

    private static User user(UUID id, String displayName, String avatarUrl, String avatarPublicId) {
        User user = new User();
        user.setId(id);
        user.setEmail("u@example.com");
        user.setUsername("user");
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.setAvatarPublicId(avatarPublicId);
        return user;
    }
}
