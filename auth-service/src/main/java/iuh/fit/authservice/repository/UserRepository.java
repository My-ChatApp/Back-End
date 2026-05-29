package iuh.fit.authservice.repository;

import iuh.fit.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    List<User> findAllByDeletedAtIsNull();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
