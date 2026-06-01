package iuh.fit.authservice.service;

import iuh.fit.authservice.dto.request.CreateUserRequest;
import iuh.fit.authservice.dto.request.UpdateUserRequest;
import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.exception.ResourceNotFoundException;
import iuh.fit.authservice.exception.UserAlreadyExistsException;
import iuh.fit.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCrudServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCrudService userCrudService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void create_duplicateEmail_throwsUserAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("a@b.com");
        request.setUsername("user1");
        request.setPassword("secret12");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userCrudService.create(request));
    }

    @Test
    void update_emailConflict_throwsUserAlreadyExists() {
        User user = activeUser("old@b.com", "user1");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@b.com")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("taken@b.com");

        assertThrows(UserAlreadyExistsException.class, () -> userCrudService.update(userId, request));
    }

    @Test
    void delete_softDeletesUser() {
        User user = activeUser("u@b.com", "user1");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userCrudService.delete(userId);

        assertFalse(user.isActive());
        assertNotNull(user.getDeletedAt());
        verify(userRepository).save(user);
    }

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userCrudService.findById(userId));
    }

    @Test
    void create_success_encodesPasswordAndSaves() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("new@b.com");
        request.setUsername("newuser");
        request.setPassword("secret12");
        when(userRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("secret12")).thenReturn("hashed");

        User saved = new User("new@b.com", "newuser", "hashed");
        saved.setId(userId);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var response = userCrudService.create(request);

        assertEquals("new@b.com", response.getEmail());
        verify(passwordEncoder).encode("secret12");
    }

    @Test
    void searchUsers_shortQuery_returnsEmpty() {
        assertTrue(userCrudService.searchUsers("a", userId, 20).isEmpty());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void searchUsers_atUsername_callsFindByUsername() {
        User user = activeUser("other@b.com", "datba");
        UUID otherId = UUID.randomUUID();
        user.setId(otherId);
        when(userRepository.findByUsername("datba")).thenReturn(Optional.of(user));
        when(userRepository.searchActiveByDisplayNameOrUsername(
                eq("datba"), eq(userId), any(Pageable.class)))
                .thenReturn(List.of());

        var results = userCrudService.searchUsers("@datba", userId, 20);

        assertEquals(1, results.size());
        assertEquals("datba", results.get(0).getUsername());
        verify(userRepository).findByUsername("datba");
    }

    @Test
    void searchUsers_email_callsFindByEmail() {
        User user = activeUser("find@b.com", "finder");
        UUID otherId = UUID.randomUUID();
        user.setId(otherId);
        when(userRepository.findByEmail("find@b.com")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("find@b.com")).thenReturn(Optional.empty());
        when(userRepository.searchActiveByDisplayNameOrUsername(
                eq("find@b.com"), eq(userId), any(Pageable.class)))
                .thenReturn(List.of());

        var results = userCrudService.searchUsers("find@b.com", userId, 20);

        assertEquals(1, results.size());
        assertEquals("find@b.com", results.get(0).getEmail());
        verify(userRepository).findByEmail("find@b.com");
    }

    @Test
    void searchUsers_excludesSelfAndInactive() {
        User self = activeUser("me@b.com", "me");
        self.setId(userId);
        when(userRepository.findByUsername("meuser")).thenReturn(Optional.of(self));

        var results = userCrudService.searchUsers("meuser", userId, 20);

        assertTrue(results.isEmpty());
    }

    @Test
    void searchUsers_fuzzyDisplayName_dedupesWithExactUsername() {
        User user = activeUser("u@b.com", "nguyen");
        UUID otherId = UUID.randomUUID();
        user.setId(otherId);
        user.setDisplayName("Nguyen Van A");
        when(userRepository.findByUsername("nguyen")).thenReturn(Optional.of(user));
        when(userRepository.searchActiveByDisplayNameOrUsername(
                eq("nguyen"), eq(userId), any(Pageable.class)))
                .thenReturn(List.of(user));

        var results = userCrudService.searchUsers("nguyen", userId, 20);

        assertEquals(1, results.size());
        assertEquals("Nguyen Van A", results.get(0).getDisplayName());
    }

    private User activeUser(String email, String username) {
        User user = new User(email, username, "hash");
        user.setId(userId);
        user.setActive(true);
        return user;
    }
}
