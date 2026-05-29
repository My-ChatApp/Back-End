package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.event.payload.ChatMessageCreatedEvent;
import iuh.fit.chatservice.event.publisher.ChatInternalEventPublisher;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatStorageStrategyTest {

    @Mock
    private ChatSpaceRepository chatSpaceRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatInternalEventPublisher internalEventPublisher;

    @Test
    void dynamoDbOnly_persist_savesWithoutPublish() {
        DynamoDbOnlyStorageStrategy strategy = new DynamoDbOnlyStorageStrategy(chatMessageRepository);
        ChatMessage message = sampleMessage();

        strategy.persistNewMessage(message, List.of("receiver-1"));

        verify(chatMessageRepository).save(message);
        verifyNoInteractions(chatSpaceRepository, internalEventPublisher);
    }

    @Test
    void valkeyAsync_persist_appendsAndPublishesWithoutSyncSave() {
        ValkeyAsyncDynamoStorageStrategy strategy = new ValkeyAsyncDynamoStorageStrategy(
                chatSpaceRepository, chatMessageRepository, internalEventPublisher);
        ChatMessage message = sampleMessage();

        strategy.persistNewMessage(message, List.of("receiver-1"));

        verify(chatSpaceRepository).appendMessage(message);
        verify(chatMessageRepository, never()).save(message);

        ArgumentCaptor<ChatMessageCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);
        verify(internalEventPublisher).publishMessageCreated(eventCaptor.capture());
        assertEquals(message.getMessageId(), eventCaptor.getValue().getMessageId());
        assertEquals(List.of("receiver-1"), eventCaptor.getValue().getReceiverIds());
    }

    @Test
    void valkeyDualWrite_persist_appendsSavesAndPublishes() {
        ValkeyDualWriteStorageStrategy strategy = new ValkeyDualWriteStorageStrategy(
                chatSpaceRepository, chatMessageRepository, internalEventPublisher);
        ChatMessage message = sampleMessage();

        strategy.persistNewMessage(message, List.of("receiver-1"));

        verify(chatSpaceRepository).appendMessage(message);
        verify(chatMessageRepository).save(message);
        verify(internalEventPublisher).publishMessageCreated(org.mockito.ArgumentMatchers.any());
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
