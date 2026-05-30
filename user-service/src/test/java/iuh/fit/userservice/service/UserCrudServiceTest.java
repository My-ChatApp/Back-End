package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.request.UpdateUserRequest;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.exception.ResourceNotFoundException;
import iuh.fit.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCrudServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCrudService userCrudService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userCrudService.findById(userId));
    }

    @Test
    void delete_softDeletesUser() {
        User user = activeUser();
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userCrudService.delete(userId);

        assertFalse(user.isActive());
        assertNotNull(user.getDeletedAt());
        verify(userRepository).save(user);
    }

    @Test
    void update_onlineTrue_setsLastSeenAt() {
        User user = activeUser();
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("New Name");
        request.setOnline(true);

        var response = userCrudService.update(userId, request);

        assertEquals("New Name", response.getDisplayName());
        assertNotNull(user.getLastSeenAt());
        verify(userRepository).save(user);
    }

    private User activeUser() {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setUsername("user1");
        user.setDisplayName("User One");
        user.setActive(true);
        return user;
    }
}
