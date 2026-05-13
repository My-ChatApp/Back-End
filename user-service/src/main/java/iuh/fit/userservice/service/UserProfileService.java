package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.request.UpdateProfileRequest;
import iuh.fit.userservice.entity.UserProfile;
import iuh.fit.userservice.exception.ImageUploadException;
import iuh.fit.userservice.exception.ProfileNotFound;
import iuh.fit.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public UserProfile updateProfile(String userId,
                                     UpdateProfileRequest request) {

        UserProfile profile = repository.findByUserId(userId);
        if(profile == null) {
            throw new ProfileNotFound("Profile not found for userId: " + userId);
        }

        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName());
        }

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            try {
                // Xóa ảnh cũ nếu có
                if (profile.getAvatarUrl() != null) {
                    String publicId = extractPublicId(profile.getAvatarUrl());
                    cloudinaryService.deleteImage(publicId);
                }

                String avatarUrl = cloudinaryService.uploadImage(request.getAvatar());
                profile.setAvatarUrl(avatarUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload avatar image", e);
            }
        }


        return repository.save(profile);
    }

    private String extractPublicId(String avatarUrl) {
        String[] parts = avatarUrl.split("/upload/");
        String path = parts[1].replaceFirst("v\\d+/", ""); // bỏ version
        return path.substring(0, path.lastIndexOf(".")); // bỏ extension
    }
}