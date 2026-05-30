package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.request.UpdateUserRequest;
import iuh.fit.userservice.dto.response.UserResponse;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.exception.ResourceNotFoundException;
import iuh.fit.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCrudService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllByDeletedAtIsNull().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return UserResponse.from(getActiveUser(id));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = getActiveUser(id);

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        ProfileFieldsSupport.applyPhone(user, request.getPhone());
        ProfileFieldsSupport.applyDateOfBirth(user, request.getDateOfBirth());
        ProfileFieldsSupport.applyGender(user, request.getGender());
        if (request.getLocale() != null) {
            user.setLocale(request.getLocale());
        }
        if (request.getOnline() != null) {
            user.setOnline(request.getOnline());
            if (request.getOnline()) {
                user.setLastSeenAt(Instant.now());
            }
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User user = getActiveUser(id);
        user.setActive(false);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    private User getActiveUser(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
