package iuh.fit.notificationservice.event.consumer;

import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.NotificationType;
import iuh.fit.notificationservice.event.payload.FriendRequestSentEvent;
import iuh.fit.notificationservice.event.payload.LoginLockoutEvent;
import iuh.fit.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void handleFriendRequest_createsSystemNotification() {
        UUID receiverId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(notificationRepository.existsByUserIdAndTypeAndReferenceIdAndDeletedFalse(
                        eq(receiverId), eq(NotificationType.FRIEND_REQUEST), eq(requestId)))
                .thenReturn(false);

        FriendRequestSentEvent event = FriendRequestSentEvent.builder()
                .requestId(requestId.toString())
                .senderId(senderId.toString())
                .receiverId(receiverId.toString())
                .senderName("Alice")
                .build();

        notificationConsumer.handleFriendRequest(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(NotificationType.FRIEND_REQUEST, saved.getType());
        assertEquals(receiverId, saved.getUserId());
        assertEquals("Alice muốn kết bạn với bạn", saved.getBody());
        assertFalse(saved.isRead());
    }

    @Test
    void handleFriendRequest_skipsDuplicate() {
        UUID receiverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(notificationRepository.existsByUserIdAndTypeAndReferenceIdAndDeletedFalse(
                        any(), any(), any()))
                .thenReturn(true);

        FriendRequestSentEvent event = FriendRequestSentEvent.builder()
                .requestId(requestId.toString())
                .senderId(UUID.randomUUID().toString())
                .receiverId(receiverId.toString())
                .senderName("Alice")
                .build();

        notificationConsumer.handleFriendRequest(event);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void handleLoginLockout_createsSystemNotification() {
        UUID userId = UUID.randomUUID();
        Instant lockedUntil = Instant.parse("2026-06-02T12:00:00Z");
        UUID referenceId = UUID.nameUUIDFromBytes(
                ("login-lockout:" + userId + ":" + lockedUntil).getBytes()
        );

        when(notificationRepository.existsByUserIdAndTypeAndReferenceIdAndDeletedFalse(
                        eq(userId), eq(NotificationType.SYSTEM), eq(referenceId)))
                .thenReturn(false);

        LoginLockoutEvent event = new LoginLockoutEvent(
                userId.toString(),
                "user@b.com",
                "203.0.113.10",
                lockedUntil,
                5
        );

        notificationConsumer.handleLoginLockout(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(NotificationType.SYSTEM, saved.getType());
        assertEquals(userId, saved.getUserId());
        assertEquals("Cảnh báo đăng nhập", saved.getTitle());
        assertTrue(saved.getBody().contains("203.0.113.10"));
        assertFalse(saved.isRead());
    }
}
