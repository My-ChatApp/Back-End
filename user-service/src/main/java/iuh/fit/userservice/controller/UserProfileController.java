package iuh.fit.userservice.controller;

import iuh.fit.common.dto.response.ApiResponse;
import iuh.fit.common.security.CurrentUser;
import iuh.fit.common.security.SecurityUtils;
import iuh.fit.userservice.dto.request.UpdateProfileRequest;
import iuh.fit.userservice.entity.UserProfile;
import iuh.fit.userservice.repository.UserProfileRepository;
import iuh.fit.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    @PatchMapping(value = "/update-profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> updateProfile(
            @RequestPart(value = "request", required = false) UpdateProfileRequest request
            ){
        String userId = SecurityUtils.getCurrentUser().getUserId();

        UserProfile updatedProfile = userProfileService.updateProfile(userId, request);
        if (updatedProfile == null) {
            return new ApiResponse<>(
                    false,
                    "Profile not found for userId: " + userId,
                    null
                    );
        }

        return new ApiResponse<>(
                true,
                "Profile updated successfully",
                null
        );
    }

}
