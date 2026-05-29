package iuh.fit.chatservice.event.consumer;

import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.event.payload.ChatMessageCreatedEvent;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.service.ChatInboxBroadcastService;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatPersistConsumerTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private ChatInboxBroadcastService inboxBroadcastService;

    @InjectMocks
    private ChatPersistConsumer chatPersistConsumer;

    private final UUID conversationId = UUID.randomUUID();
    private final UUID senderId = UUID.randomUUID();
    private final String messageId = UUID.randomUUID().toString();

    @Test
    void handleMessageCreated_alreadyExists_skipsSave() {
        ChatMessageCreatedEvent event = baseEvent();
        when(chatMessageRepository.existsByMessageId(messageId)).thenReturn(true);

        chatPersistConsumer.handleMessageCreated(event);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void handleMessageCreated_newMessage_savesToDynamo() {
        ChatMessageCreatedEvent event = baseEvent();
        when(chatMessageRepository.existsByMessageId(messageId)).thenReturn(false);
        Conversation conversation = Conversation.builder().id(conversationId).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        chatPersistConsumer.handleMessageCreated(event);

        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(conversationRepository).save(conversation);
    }

    private ChatMessageCreatedEvent baseEvent() {
        return ChatMessageCreatedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId.toString())
                .senderId(senderId.toString())
                .type(MessageType.TEXT)
                .content("hello")
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .receiverIds(java.util.List.of())
                .build();
    }
}
