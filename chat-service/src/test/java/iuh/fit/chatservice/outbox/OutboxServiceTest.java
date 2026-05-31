package iuh.fit.chatservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.chatservice.config.ChatOutboxProperties;
import iuh.fit.chatservice.entity.OutboxEvent;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.entity.enums.OutboxEventStatus;
import iuh.fit.chatservice.entity.enums.OutboxEventType;
import iuh.fit.chatservice.event.payload.ChatMessageCreatedEvent;
import iuh.fit.chatservice.event.publisher.ChatInternalEventPublisher;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ChatInternalEventPublisher internalEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ChatOutboxProperties properties = new ChatOutboxProperties();

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        outboxService = new OutboxService(outboxEventRepository, internalEventPublisher, objectMapper, properties);
        ReflectionTestUtils.setField(outboxService, "persistRoutingKey", "chat.message.created");
        ReflectionTestUtils.setField(outboxService, "updatedRoutingKey", "chat.message.updated");
        ReflectionTestUtils.setField(outboxService, "hydrateRoutingKey", "chat.history.load");
    }

    @Test
    void enqueueMessageCreated_disabled_publishesDirectly() {
        properties.setEnabled(false);
        ChatMessage message = sampleMessage();

        outboxService.enqueueMessageCreated(message, List.of("receiver-1"));

        verify(internalEventPublisher).publishMessageCreated(any(ChatMessageCreatedEvent.class));
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void enqueueMessageCreated_enabled_publishesAndMarksPublished() {
        ChatMessage message = sampleMessage();
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent row = invocation.getArgument(0);
            if (row.getId() == null) {
                row.setId(UUID.randomUUID());
            }
            return row;
        });

        outboxService.enqueueMessageCreated(message, List.of("receiver-1"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        OutboxEvent saved = captor.getAllValues().getLast();
        assertEquals(OutboxEventStatus.PUBLISHED, saved.getStatus());
        assertEquals(OutboxEventType.MESSAGE_CREATED, saved.getEventType());
        verify(internalEventPublisher).publishMessageCreated(any(ChatMessageCreatedEvent.class));
    }

    @Test
    void enqueueMessageCreated_publishFails_keepsPendingWithRetry() {
        ChatMessage message = sampleMessage();
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent row = invocation.getArgument(0);
            row.setId(UUID.randomUUID());
            return row;
        });
        doThrow(new RuntimeException("broker down"))
                .when(internalEventPublisher)
                .publishMessageCreated(any());

        outboxService.enqueueMessageCreated(message, List.of("receiver-1"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        OutboxEvent saved = captor.getAllValues().getLast();
        assertEquals(OutboxEventStatus.PENDING, saved.getStatus());
        assertEquals(1, saved.getRetryCount());
    }

    @Test
    void tryPublish_skipsWhenAlreadyPublished() {
        UUID id = UUID.randomUUID();
        OutboxEvent row = OutboxEvent.builder()
                .id(id)
                .status(OutboxEventStatus.PUBLISHED)
                .build();
        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(row));

        outboxService.tryPublish(id);

        verify(internalEventPublisher, never()).publishMessageCreated(any());
    }

    private static ChatMessage sampleMessage() {
        return ChatMessage.builder()
                .messageId("msg-1")
                .conversationId("conv-1")
                .senderId("user-1")
                .type(MessageType.TEXT)
                .content("hello")
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();
    }
}
