package iuh.fit.userservice.service;

import iuh.fit.userservice.config.MediaProperties;
import iuh.fit.userservice.dto.request.UpdateProfileRequest;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.exception.ImageUploadException;
import iuh.fit.userservice.exception.ProfileNotFound;
import iuh.fit.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final S3ObjectStorageService s3ObjectStorageService;

    @Transactional
    public User updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ProfileNotFound("User not found for userId: " + userId));

        if (request != null && request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        if (request != null) {
            ProfileFieldsSupport.applyPhone(user, request.getPhone());
            ProfileFieldsSupport.applyDateOfBirth(user, request.getDateOfBirth());
            ProfileFieldsSupport.applyGender(user, request.getGender());
        }

        if (request != null && request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            try {
                deletePreviousAvatar(user);
                S3ObjectStorageService.UploadResult result =
                        s3ObjectStorageService.uploadAvatar(userId, request.getAvatar());
                user.setAvatarUrl(result.publicUrl());
                user.setAvatarPublicId(result.key());
            } catch (ImageUploadException | IllegalArgumentException e) {
                throw new ImageUploadException(
                        e.getMessage() != null ? e.getMessage() : "Failed to upload avatar image",
                        e instanceof ImageUploadException ? e.getCause() : e);
            }
        } else if (request != null && request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            deletePreviousAvatar(user);
            user.setAvatarUrl(request.getAvatarUrl().trim());
            if (request.getAvatarS3Key() != null && !request.getAvatarS3Key().isBlank()) {
                user.setAvatarPublicId(request.getAvatarS3Key().trim());
            } else {
                String key = s3ObjectStorageService.keyFromPublicUrl(request.getAvatarUrl());
                if (key != null && !key.isBlank()) {
                    user.setAvatarPublicId(key);
                }
            }
        }

        return userRepository.save(user);
    }

    private void deletePreviousAvatar(User user) {
        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isBlank()) {
            if (!MediaProperties.DEFAULT_AVATAR_KEY.equals(user.getAvatarPublicId())) {
                s3ObjectStorageService.deleteIfExists(user.getAvatarPublicId());
            }
            return;
        }
        if (s3ObjectStorageService.isManagedPublicUrl(user.getAvatarUrl())) {
            String key = s3ObjectStorageService.keyFromPublicUrl(user.getAvatarUrl());
            s3ObjectStorageService.deleteIfExists(key);
        }
    }
}
