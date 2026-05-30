package iuh.fit.chatservice.event.consumer;

import iuh.fit.chatservice.config.ChatSpaceProperties;
import iuh.fit.chatservice.event.payload.ConversationHistoryLoadRequestedEvent;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHydrationConsumerTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatSpaceRepository chatSpaceRepository;
    @Mock
    private ChatSpaceProperties chatSpaceProperties;

    @InjectMocks
    private ChatHydrationConsumer chatHydrationConsumer;

    @Test
    void handleHistoryLoad_withMessages_appendsBatchAndReleasesLock() {
        when(chatSpaceProperties.getHydrateBatchSize()).thenReturn(50);
        ChatMessage msg = ChatMessage.builder()
                .messageId("m1")
                .conversationId("conv-1")
                .build();
        when(chatMessageRepository.findByConversationIdBefore(
                eq("conv-1"), any(), any(), anyInt())).thenReturn(List.of(msg));

        ConversationHistoryLoadRequestedEvent event = ConversationHistoryLoadRequestedEvent.builder()
                .conversationId("conv-1")
                .beforeMessageId("before-1")
                .beforeCreatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                .limit(100)
                .build();

        chatHydrationConsumer.handleHistoryLoad(event);

        verify(chatSpaceRepository).appendMessagesBatch(List.of(msg));
        verify(chatSpaceRepository).releaseHydrateLock("conv-1");
    }

    @Test
    void handleHistoryLoad_onFailure_stillReleasesLock() {
        when(chatSpaceProperties.getHydrateBatchSize()).thenReturn(50);
        when(chatMessageRepository.findByConversationIdBefore(
                any(), any(), any(), anyInt())).thenThrow(new RuntimeException("dynamo down"));

        ConversationHistoryLoadRequestedEvent event = ConversationHistoryLoadRequestedEvent.builder()
                .conversationId("conv-1")
                .limit(10)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> chatHydrationConsumer.handleHistoryLoad(event));

        verify(chatSpaceRepository).releaseHydrateLock("conv-1");
    }
}
