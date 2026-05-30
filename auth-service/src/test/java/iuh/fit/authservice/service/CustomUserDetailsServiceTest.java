package iuh.fit.authservice.service;

import iuh.fit.authservice.entity.User;
import iuh.fit.authservice.repository.UserRepository;
import iuh.fit.authservice.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFound() {
        when(userRepository.findByEmail("missing@b.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@b.com"));
    }

    @Test
    void loadUserByUsername_found_returnsCustomUserDetails() {
        UUID id = UUID.randomUUID();
        User user = new User("user@b.com", "user1", "hashed-password");
        user.setId(id);
        when(userRepository.findByEmail("user@b.com")).thenReturn(Optional.of(user));

        var details = customUserDetailsService.loadUserByUsername("user@b.com");

        assertInstanceOf(CustomUserDetails.class, details);
        CustomUserDetails custom = (CustomUserDetails) details;
        assertEquals(id.toString(), custom.getUserid());
        assertEquals("user@b.com", custom.getUsername());
        assertEquals("hashed-password", custom.getPassword());
    }
}
