package iuh.fit.notificationservice.repository;

import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByUserIdAndTypeAndReferenceIdAndDeletedFalse(
            UUID userId, NotificationType type, UUID referenceId);

    Optional<Notification> findByUserIdAndTypeAndReferenceIdAndDeletedFalse(
            UUID userId, NotificationType type, UUID referenceId);

    Optional<Notification> findByIdAndDeletedFalse(UUID id);

    List<Notification> findByDeletedFalseOrderByCreatedAtDesc();

    List<Notification> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalseAndDeletedFalse(UUID userId);
}
