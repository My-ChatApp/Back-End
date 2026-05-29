package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.request.CreateFriendRequestRequest;
import iuh.fit.userservice.entity.friend.FriendRequest;
import iuh.fit.userservice.entity.friend.FriendRequestStatus;
import iuh.fit.userservice.event.payload.FriendAcceptedEvent;
import iuh.fit.userservice.event.payload.FriendRequestSentEvent;
import iuh.fit.userservice.exception.ResourceNotFoundException;
import iuh.fit.userservice.repository.FriendRequestRepository;
import iuh.fit.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private FriendRequestService friendRequestService;

    private final UUID senderId = UUID.randomUUID();
    private final UUID receiverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(friendRequestService, "friendExchange", "test.friend.exchange");
        ReflectionTestUtils.setField(friendRequestService, "friendRequestKey", "friend.request");
        ReflectionTestUtils.setField(friendRequestService, "friendAcceptedKey", "friend.accepted");
    }

    @Test
    void sendRequest_selfUser_throwsIllegalState() {
        CreateFriendRequestRequest request = new CreateFriendRequestRequest(
                senderId.toString(), senderId.toString(), null);

        assertThrows(IllegalStateException.class, () -> friendRequestService.sendRequest(request));
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void sendRequest_duplicatePending_throwsIllegalState() {
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                senderId, receiverId, FriendRequestStatus.PENDING)).thenReturn(true);

        CreateFriendRequestRequest request = new CreateFriendRequestRequest(
                senderId.toString(), receiverId.toString(), "hi");

        assertThrows(IllegalStateException.class, () -> friendRequestService.sendRequest(request));
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void sendRequest_success_savesAndPublishes() {
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                senderId, receiverId, FriendRequestStatus.PENDING)).thenReturn(false);

        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        FriendRequest saved = FriendRequest.builder()
                .id(UUID.randomUUID())
                .senderId(senderId)
                .receiverId(receiverId)
                .status(FriendRequestStatus.PENDING)
                .createdAt(now)
                .build();
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(saved);
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());

        CreateFriendRequestRequest request = new CreateFriendRequestRequest(
                senderId.toString(), receiverId.toString(), "hello");
        FriendRequest result = friendRequestService.sendRequest(request);

        assertEquals(saved.getId(), result.getId());
        ArgumentCaptor<FriendRequestSentEvent> captor = ArgumentCaptor.forClass(FriendRequestSentEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("test.friend.exchange"), eq("friend.request"), captor.capture());
        assertEquals(saved.getId().toString(), captor.getValue().getRequestId());
    }

    @Test
    void acceptRequest_wrongReceiver_throwsIllegalState() {
        UUID requestId = UUID.randomUUID();
        FriendRequest pending = FriendRequest.builder()
                .id(requestId)
                .senderId(senderId)
                .receiverId(receiverId)
                .status(FriendRequestStatus.PENDING)
                .build();
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(pending));

        UUID wrongReceiver = UUID.randomUUID();
        assertThrows(IllegalStateException.class,
                () -> friendRequestService.acceptRequest(requestId, wrongReceiver));
    }

    @Test
    void unfriend_noFriendship_throwsResourceNotFound() {
        when(friendRequestRepository.findAcceptedBetween(senderId, receiverId, FriendRequestStatus.ACCEPTED))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> friendRequestService.unfriend(senderId, receiverId));
    }

    @Test
    void acceptRequest_success_publishesFriendAccepted() {
        UUID requestId = UUID.randomUUID();
        FriendRequest pending = FriendRequest.builder()
                .id(requestId)
                .senderId(senderId)
                .receiverId(receiverId)
                .status(FriendRequestStatus.PENDING)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(pending));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(receiverId)).thenReturn(Optional.empty());

        FriendRequest result = friendRequestService.acceptRequest(requestId, receiverId);

        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());
        ArgumentCaptor<FriendAcceptedEvent> captor = ArgumentCaptor.forClass(FriendAcceptedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("test.friend.exchange"), eq("friend.accepted"), captor.capture());
        assertEquals(requestId.toString(), captor.getValue().getRequestId());
    }
}
