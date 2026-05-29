package iuh.fit.notificationservice.event.consumer;

import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.NotificationType;
import iuh.fit.notificationservice.event.payload.FriendRequestSentEvent;
import iuh.fit.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
