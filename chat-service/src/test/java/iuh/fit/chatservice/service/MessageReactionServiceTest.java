package iuh.fit.chatservice.service;

import iuh.fit.chatservice.entity.enums.ReactionType;
import iuh.fit.chatservice.event.payload.ChatMessageReactionsUpdatedEvent;
import iuh.fit.chatservice.event.payload.ChatRealtimeEnvelope;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.model.MessageReactionDto;
import iuh.fit.chatservice.outbox.OutboxService;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageReactionServiceTest {

    @Mock
    private ChatSpaceRepository chatSpaceRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private ChatRealtimeBroadcastService realtimeBroadcastService;

    @InjectMocks
    private MessageReactionService messageReactionService;

    private final UUID conversationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String messageId = UUID.randomUUID().toString();

    @Test
    void setReaction_addsReaction() {
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)).thenReturn(true);
        ChatMessage message = baseMessage();
        when(chatSpaceRepository.getMessage(messageId)).thenReturn(Optional.of(message));

        ChatMessage result = messageReactionService.setReaction(
                conversationId.toString(), messageId, userId.toString(), "LIKE");

        assertEquals(1, result.getReactionCount());
        assertEquals(ReactionType.LIKE.name(), result.getReactions().get(0).getReactionType());
        verify(chatSpaceRepository).updateMessage(message);
        verify(outboxService).enqueueMessageReactionsUpdated(any(ChatMessageReactionsUpdatedEvent.class));
        verify(realtimeBroadcastService).broadcast(
                eq(conversationId.toString()), any(ChatRealtimeEnvelope.class));
    }

    @Test
    void setReaction_sameType_removesReaction() {
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)).thenReturn(true);
        ChatMessage message = baseMessage();
        ArrayList<MessageReactionDto> reactions = new ArrayList<>();
        reactions.add(MessageReactionDto.builder()
                .userId(userId.toString())
                .reactionType(ReactionType.LIKE.name())
                .createdAt(Instant.now())
                .build());
        message.setReactions(reactions);
        message.setReactionCount(1);
        when(chatSpaceRepository.getMessage(messageId)).thenReturn(Optional.of(message));

        ChatMessage result = messageReactionService.setReaction(
                conversationId.toString(), messageId, userId.toString(), "LIKE");

        assertEquals(0, result.getReactionCount());
        assertEquals(0, result.getReactions().size());
    }

    @Test
    void setReaction_notMember_throws() {
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)).thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> messageReactionService.setReaction(
                        conversationId.toString(), messageId, userId.toString(), "LOVE"));
    }

    private ChatMessage baseMessage() {
        return ChatMessage.builder()
                .messageId(messageId)
                .conversationId(conversationId.toString())
                .senderId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .reactions(new ArrayList<>())
                .build();
    }
}
