package iuh.fit.authservice.service;

import iuh.fit.authservice.dto.request.CreateUserRequest;
import iuh.fit.authservice.dto.request.UpdateUserRequest;
import iuh.fit.authservice.dto.response.UserResponse;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.exception.ResourceNotFoundException;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserCrudService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_SEARCH_LENGTH = 2;
    private static final int MAX_SEARCH_LIMIT = 50;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> findAllActive() {
        return userRepository.findAllByDeletedAtIsNull().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return UserResponse.from(getActiveUser(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String q, UUID excludeUserId, int limit) {
        if (q == null) {
            return List.of();
        }
        String trimmed = q.trim();
        if (trimmed.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        int cap = Math.min(Math.max(limit, 1), MAX_SEARCH_LIMIT);
        String handle = trimmed.startsWith("@") ? trimmed.substring(1).trim() : trimmed;
        if (handle.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        Map<UUID, User> merged = new LinkedHashMap<>();

        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            userRepository.findByEmail(trimmed.toLowerCase())
                    .ifPresent(user -> addIfSearchable(merged, user, excludeUserId, cap));
        }

        userRepository.findByUsername(handle)
                .ifPresent(user -> addIfSearchable(merged, user, excludeUserId, cap));

        if (merged.size() < cap) {
            List<User> fuzzy = userRepository.searchActiveByDisplayNameOrUsername(
                    handle,
                    excludeUserId,
                    PageRequest.of(0, cap));
            for (User user : fuzzy) {
                addIfSearchable(merged, user, excludeUserId, cap);
                if (merged.size() >= cap) {
                    break;
                }
            }
        }

        return merged.values().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        User user = new User(
                request.getEmail(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword())
        );
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        user.setBio(request.getBio());
        user.setPhone(request.getPhone());
        if (request.getLocale() != null && !request.getLocale().isBlank()) {
            user.setLocale(request.getLocale());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = getActiveUser(id);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getLocale() != null) {
            user.setLocale(request.getLocale());
        }
        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }
        if (request.getOnline() != null) {
            user.setOnline(request.getOnline());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
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

    private static boolean isSearchable(User user, UUID excludeUserId) {
        if (user == null || user.getDeletedAt() != null || !user.isActive()) {
            return false;
        }
        return excludeUserId == null || !excludeUserId.equals(user.getId());
    }

    private static void addIfSearchable(
            Map<UUID, User> merged, User user, UUID excludeUserId, int cap) {
        if (merged.size() >= cap || !isSearchable(user, excludeUserId)) {
            return;
        }
        merged.putIfAbsent(user.getId(), user);
    }
}
