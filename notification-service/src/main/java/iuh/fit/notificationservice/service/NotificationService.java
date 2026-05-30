package iuh.fit.notificationservice.service;

import iuh.fit.notificationservice.dto.request.CreateNotificationRequest;
import iuh.fit.notificationservice.dto.request.UpdateNotificationRequest;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.NotificationType;
import iuh.fit.notificationservice.exception.ResourceNotFoundException;
import iuh.fit.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<Notification> findAll() {
        return notificationRepository.findByDeletedFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Notification findById(UUID id) {
        return notificationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Notification> getByUserId(String userId) {
        return notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                        UUID.fromString(userId))
                .stream()
                .filter(n -> n.getType() != NotificationType.MESSAGE)
                .toList();
    }

    public long countUnread(String userId) {
        return notificationRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID.fromString(userId))
                .stream()
                .filter(n -> !n.isRead() && n.getType() != NotificationType.MESSAGE)
                .count();
    }

    @Transactional
    public Notification create(CreateNotificationRequest request) {
        if (request.getType() == NotificationType.MESSAGE) {
            throw new IllegalArgumentException(
                    "MESSAGE notifications are handled by chat-service (conversation inbox), not notification-service");
        }
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .actorId(request.getActorId())
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .referenceId(request.getReferenceId())
                .data(request.getData() != null ? request.getData() : Map.of())
                .read(false)
                .expireAt(request.getExpireAt() != null
                        ? request.getExpireAt()
                        : Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification update(UUID id, UpdateNotificationRequest request) {
        Notification notification = findById(id);

        if (request.getTitle() != null) {
            notification.setTitle(request.getTitle());
        }
        if (request.getBody() != null) {
            notification.setBody(request.getBody());
        }
        if (request.getData() != null) {
            notification.setData(request.getData());
        }
        if (request.getRead() != null) {
            notification.setRead(request.getRead());
            notification.setReadAt(request.getRead() ? Instant.now() : null);
        }

        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(String notificationId) {
        markAsRead(UUID.fromString(notificationId));
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = findById(notificationId);
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
    }

    /**
     * Marks all non-MESSAGE notifications as read for the user (system / friend alerts).
     */
    @Transactional
    public int markAllSystemAsRead(String userId) {
        UUID uid = UUID.fromString(userId);
        Instant now = Instant.now();
        List<Notification> unread = notificationRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(uid)
                .stream()
                .filter(n -> !n.isRead() && n.getType() != NotificationType.MESSAGE)
                .toList();
        if (unread.isEmpty()) {
            return 0;
        }
        for (Notification notification : unread) {
            notification.setRead(true);
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Transactional
    public void delete(UUID id) {
        Notification notification = findById(id);
        notification.setDeleted(true);
        notificationRepository.save(notification);
    }
}
