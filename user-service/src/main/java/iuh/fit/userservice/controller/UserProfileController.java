package iuh.fit.userservice.controller;

import iuh.fit.common.dto.response.ApiResponse;
import iuh.fit.common.security.SecurityUtils;
import iuh.fit.userservice.dto.request.UpdateProfileRequest;
import iuh.fit.userservice.dto.request.UpdateUserRequest;
import iuh.fit.userservice.dto.response.UserResponse;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.service.UserCrudService;
import iuh.fit.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserCrudService userCrudService;

    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return new ApiResponse<>(true, "OK", userCrudService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return new ApiResponse<>(true, "OK", userCrudService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        return new ApiResponse<>(true, "User updated", userCrudService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        userCrudService.delete(id);
        return new ApiResponse<>(true, "User deleted", null);
    }

    @PatchMapping(value = "/update-profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> updateProfile(
            @RequestPart(value = "request", required = false) UpdateProfileRequest request) {
        String userId = SecurityUtils.getCurrentUser().getUserId();
        User updated = userProfileService.updateProfile(userId, request);
        return new ApiResponse<>(true, "Profile updated successfully", UserResponse.from(updated));
    }
}
