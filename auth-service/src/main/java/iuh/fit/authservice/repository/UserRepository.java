package iuh.fit.authservice.repository;

import iuh.fit.authservice.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL AND u.active = true
              AND (:excludeId IS NULL OR u.id <> :excludeId)
              AND (
                LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    List<User> searchActiveByDisplayNameOrUsername(
            @Param("q") String q,
            @Param("excludeId") UUID excludeId,
            Pageable pageable);
}
