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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
