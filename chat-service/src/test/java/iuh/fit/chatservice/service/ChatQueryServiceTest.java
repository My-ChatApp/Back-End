package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.response.MessageSearchResult;
import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.exception.ForbiddenException;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.storage.ChatStorageStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceTest {

    @Mock
    private ChatStorageStrategy chatStorageStrategy;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ObjectProvider<iuh.fit.chatservice.space.ChatSpaceRepository> chatSpaceRepositoryProvider;

    @InjectMocks
    private ChatQueryService chatQueryService;

    private final UUID conversationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void getMessages_notMember_throws() {
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> chatQueryService.getMessages(conversationId.toString(), userId.toString(), 20, null));
    }

    @Test
    void getMessages_member_delegatesToStorage() {
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)).thenReturn(true);
        MessagesPageResponse page = MessagesPageResponse.builder()
                .messages(List.of())
                .build();
        when(chatStorageStrategy.getMessages(
                conversationId.toString(), userId.toString(), 20, null)).thenReturn(page);

        MessagesPageResponse result = chatQueryService.getMessages(
                conversationId.toString(), userId.toString(), 20, null);

        assertEquals(page, result);
        verify(chatStorageStrategy).getMessages(conversationId.toString(), userId.toString(), 20, null);
    }

    @Test
    void searchMessages_notMember_throwsForbidden() {
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> chatQueryService.searchMessages(
                        conversationId.toString(), userId.toString(), "hello", 10));
    }

    @Test
    void searchMessages_member_returnsMappedResults() {
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)).thenReturn(true);
        ChatMessage msg = ChatMessage.builder()
                .messageId("m1")
                .conversationId(conversationId.toString())
                .senderId(userId.toString())
                .type(MessageType.TEXT)
                .content("hello world")
                .build();
        when(chatMessageRepository.searchByConversationId(conversationId.toString(), "hello", 10))
                .thenReturn(List.of(msg));

        List<MessageSearchResult> results = chatQueryService.searchMessages(
                conversationId.toString(), userId.toString(), "hello", 10);

        assertEquals(1, results.size());
        assertEquals("m1", results.get(0).getMessageId());
        assertEquals("hello world", results.get(0).getContent());
    }

    @Test
    void getMessagesAround_member_loadsWindowAndHydratesValkey() {
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)).thenReturn(true);

        Instant anchorTime = Instant.parse("2024-01-15T10:00:00Z");
        ChatMessage anchor = ChatMessage.builder()
                .messageId("anchor")
                .conversationId(conversationId.toString())
                .senderId(userId.toString())
                .type(MessageType.TEXT)
                .content("target")
                .createdAt(anchorTime)
                .build();
        ChatMessage older = ChatMessage.builder()
                .messageId("older")
                .conversationId(conversationId.toString())
                .senderId(userId.toString())
                .type(MessageType.TEXT)
                .content("old")
                .createdAt(anchorTime.minusSeconds(60))
                .build();

        when(chatMessageRepository.findByMessageId("anchor")).thenReturn(Optional.of(anchor));
        when(chatMessageRepository.findByConversationIdBefore(
                eq(conversationId.toString()), eq(anchorTime), eq("anchor"), eq(20)))
                .thenReturn(List.of(older));
        when(chatMessageRepository.findByConversationIdAfter(
                eq(conversationId.toString()), eq(anchorTime), eq("anchor"), eq(20)))
                .thenReturn(List.of());
        ChatMessage evenOlder = ChatMessage.builder()
                .messageId("even-older")
                .conversationId(conversationId.toString())
                .senderId(userId.toString())
                .type(MessageType.TEXT)
                .content("older still")
                .createdAt(anchorTime.minusSeconds(120))
                .build();
        when(chatMessageRepository.findByConversationIdBefore(
                eq(conversationId.toString()), eq(older.getCreatedAt()), eq("older"), eq(1)))
                .thenReturn(List.of(evenOlder));

        iuh.fit.chatservice.space.ChatSpaceRepository space =
                org.mockito.Mockito.mock(iuh.fit.chatservice.space.ChatSpaceRepository.class);
        when(chatSpaceRepositoryProvider.getIfAvailable()).thenReturn(space);

        MessagesPageResponse page = chatQueryService.getMessagesAround(
                conversationId.toString(), userId.toString(), "anchor", 40);

        assertEquals(2, page.getMessages().size());
        assertEquals("older", page.getMessages().get(0).getMessageId());
        assertEquals("anchor", page.getMessages().get(1).getMessageId());
        assertTrue(page.isHasMore());
        verify(space).appendMessagesBatch(any());
    }
}
