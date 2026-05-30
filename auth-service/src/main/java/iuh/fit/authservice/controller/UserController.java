package iuh.fit.authservice.controller;

import iuh.fit.authservice.dto.request.CreateUserRequest;
import iuh.fit.authservice.dto.request.UpdateUserRequest;
import iuh.fit.authservice.dto.response.UserResponse;
import iuh.fit.authservice.service.UserCrudService;
import iuh.fit.common.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCrudService userCrudService;

    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return new ApiResponse<>(true, "OK", userCrudService.findAllActive());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return new ApiResponse<>(true, "OK", userCrudService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return new ApiResponse<>(true, "User created", userCrudService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return new ApiResponse<>(true, "User updated", userCrudService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        userCrudService.delete(id);
        return new ApiResponse<>(true, "User deleted", null);
    }
}
