package iuh.fit.notificationservice.repository;

import iuh.fit.notificationservice.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndIsRead(String userId, boolean isRead);
}