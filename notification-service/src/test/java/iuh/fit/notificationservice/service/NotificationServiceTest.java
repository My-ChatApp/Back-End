package iuh.fit.notificationservice.service;

import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.exception.ResourceNotFoundException;
import iuh.fit.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private final UUID notificationId = UUID.randomUUID();

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(notificationRepository.findByIdAndDeletedFalse(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.findById(notificationId));
    }

    @Test
    void markAsRead_setsReadAt() {
        Notification notification = Notification.builder()
                .id(notificationId)
                .read(false)
                .build();
        when(notificationRepository.findByIdAndDeletedFalse(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(notificationId);

        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void delete_softDeletes() {
        Notification notification = Notification.builder()
                .id(notificationId)
                .deleted(false)
                .build();
        when(notificationRepository.findByIdAndDeletedFalse(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.delete(notificationId);

        assertTrue(notification.isDeleted());
        verify(notificationRepository).save(notification);
    }
}
