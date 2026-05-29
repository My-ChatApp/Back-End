package iuh.fit.notificationservice.repository;

import iuh.fit.notificationservice.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPresenceRepository extends JpaRepository<UserPresence, UUID> {
}
