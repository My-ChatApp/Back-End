package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.outbox.OutboxService;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

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
    private OutboxService outboxService;

    @Test
    void dynamoDbOnly_persist_savesWithoutPublish() {
        DynamoDbOnlyStorageStrategy strategy = new DynamoDbOnlyStorageStrategy(chatMessageRepository);
        ChatMessage message = sampleMessage();

        strategy.persistNewMessage(message, List.of("receiver-1"));

        verify(chatMessageRepository).save(message);
        verifyNoInteractions(chatSpaceRepository, outboxService);
    }

    @Test
    void valkeyAsync_persist_appendsAndEnqueuesWithoutSyncSave() {
        ValkeyAsyncDynamoStorageStrategy strategy = new ValkeyAsyncDynamoStorageStrategy(
                chatSpaceRepository, chatMessageRepository, outboxService);
        ChatMessage message = sampleMessage();

        strategy.persistNewMessage(message, List.of("receiver-1"));

        verify(chatSpaceRepository).appendMessage(message);
        verify(chatMessageRepository, never()).save(message);
        verify(outboxService).enqueueMessageCreated(message, List.of("receiver-1"));
    }

    @Test
    void valkeyDualWrite_persist_appendsSavesAndEnqueues() {
        ValkeyDualWriteStorageStrategy strategy = new ValkeyDualWriteStorageStrategy(
                chatSpaceRepository, chatMessageRepository, outboxService);
        ChatMessage message = sampleMessage();

        strategy.persistNewMessage(message, List.of("receiver-1"));

        verify(chatSpaceRepository).appendMessage(message);
        verify(chatMessageRepository).save(message);
        verify(outboxService).enqueueMessageCreated(message, List.of("receiver-1"));
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
