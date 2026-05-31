package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.outbox.OutboxService;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractValkeyStorageStrategyGetMessagesTest {

    private static final String CONV_ID = "conv-1";
    private static final String USER_ID = "user-1";
    private static final String BEFORE_ID = "before-msg";
    private static final Instant BEFORE_AT = Instant.parse("2025-06-01T12:00:00Z");

    @Mock
    private ChatSpaceRepository chatSpaceRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private OutboxService outboxService;

    private ValkeyAsyncDynamoStorageStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ValkeyAsyncDynamoStorageStrategy(
                chatSpaceRepository, chatMessageRepository, outboxService);
    }

    @Test
    void getMessages_withBefore_noOlderInDdb_returnsHasMoreFalse() {
        when(chatSpaceRepository.getBefore(CONV_ID, BEFORE_ID, 20)).thenReturn(List.of());
        when(chatSpaceRepository.getMessage(BEFORE_ID)).thenReturn(Optional.of(cursorMessage()));
        when(chatMessageRepository.findByConversationIdBefore(
                eq(CONV_ID), eq(BEFORE_AT), eq(BEFORE_ID), eq(1)))
                .thenReturn(List.of());

        MessagesPageResponse result = strategy.getMessages(CONV_ID, USER_ID, 20, BEFORE_ID);

        assertFalse(result.isLoading());
        assertFalse(result.isHasMore());
        assertTrue(result.getMessages().isEmpty());
        verify(outboxService, never()).enqueueHistoryLoadRequested(any());
    }

    @Test
    void getMessages_withBefore_olderInDdb_triggersHydrate() {
        ChatMessage older = sampleMessage("older-1");
        when(chatSpaceRepository.getBefore(CONV_ID, BEFORE_ID, 20)).thenReturn(List.of());
        when(chatSpaceRepository.getMessage(BEFORE_ID)).thenReturn(Optional.of(cursorMessage()));
        when(chatMessageRepository.findByConversationIdBefore(
                eq(CONV_ID), eq(BEFORE_AT), eq(BEFORE_ID), eq(1)))
                .thenReturn(List.of(older));
        when(chatSpaceRepository.tryAcquireHydrateLock(CONV_ID, 30)).thenReturn(true);

        MessagesPageResponse result = strategy.getMessages(CONV_ID, USER_ID, 20, BEFORE_ID);

        assertTrue(result.isLoading());
        assertTrue(result.isHasMore());
        verify(outboxService).enqueueHistoryLoadRequested(any());
    }

    @Test
    void getMessages_withBefore_partialCache_noOlderInDdb_returnsPartialAndHasMoreFalse() {
        List<ChatMessage> partial = List.of(
                sampleMessage("m1"),
                sampleMessage("m2"),
                sampleMessage("m3"),
                sampleMessage("m4"),
                sampleMessage("m5"));
        when(chatSpaceRepository.getBefore(CONV_ID, BEFORE_ID, 20)).thenReturn(partial);
        when(chatSpaceRepository.getMessage(BEFORE_ID)).thenReturn(Optional.of(cursorMessage()));
        when(chatMessageRepository.findByConversationIdBefore(
                eq(CONV_ID), eq(BEFORE_AT), eq(BEFORE_ID), eq(1)))
                .thenReturn(List.of());

        MessagesPageResponse result = strategy.getMessages(CONV_ID, USER_ID, 20, BEFORE_ID);

        assertFalse(result.isLoading());
        assertFalse(result.isHasMore());
        assertEquals(5, result.getMessages().size());
        verify(outboxService, never()).enqueueHistoryLoadRequested(any());
    }

    private static ChatMessage cursorMessage() {
        return ChatMessage.builder()
                .messageId(BEFORE_ID)
                .conversationId(CONV_ID)
                .senderId(USER_ID)
                .type(MessageType.TEXT)
                .content("cursor")
                .createdAt(BEFORE_AT)
                .build();
    }

    private static ChatMessage sampleMessage(String id) {
        return ChatMessage.builder()
                .messageId(id)
                .conversationId(CONV_ID)
                .senderId(USER_ID)
                .type(MessageType.TEXT)
                .content("hello")
                .createdAt(BEFORE_AT.minusSeconds(60))
                .build();
    }
}
