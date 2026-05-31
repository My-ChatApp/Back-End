package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.SendMessageRequest;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.entity.enums.MemberRole;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.outbox.OutboxService;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.repository.ConversationRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import iuh.fit.chatservice.storage.ChatStorageStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatCommandServiceTest {

    @Mock
    private ChatStorageStrategy chatStorageStrategy;
    @Mock
    private ChatSpaceRepository chatSpaceRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationMemberRepository conversationMemberRepository;
    @Mock
    private ChatRealtimeBroadcastService realtimeBroadcastService;
    @Mock
    private ChatInboxBroadcastService inboxBroadcastService;
    @Mock
    private MessageAttachmentMapper messageAttachmentMapper;

    @InjectMocks
    private ChatCommandService chatCommandService;

    private final UUID conversationId = UUID.randomUUID();
    private final UUID senderId = UUID.randomUUID();
    private final UUID receiverId = UUID.randomUUID();

    @Test
    void sendMessage_notMember_throws() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(new Conversation()));
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, senderId)).thenReturn(false);

        SendMessageRequest req = new SendMessageRequest();
        req.setConversationId(conversationId.toString());
        req.setSenderId(senderId.toString());
        req.setContent("hi");

        assertThrows(RuntimeException.class, () -> chatCommandService.sendMessage(req));
    }

    @Test
    void sendMessage_success_persistsAndBroadcasts() {
        Conversation conversation = Conversation.builder().id(conversationId).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, senderId)).thenReturn(true);
        when(messageAttachmentMapper.toDtoList(any())).thenReturn(List.of());
        when(messageAttachmentMapper.resolveType(any(), any())).thenReturn(MessageType.TEXT);
        doNothing().when(messageAttachmentMapper).validate(any(), any(), any());

        ConversationMember receiver = ConversationMember.of(conversation, receiverId, MemberRole.MEMBER);
        when(conversationMemberRepository.findById_ConversationIdAndDeletedFalse(conversationId))
                .thenReturn(List.of(receiver));

        SendMessageRequest req = new SendMessageRequest();
        req.setConversationId(conversationId.toString());
        req.setSenderId(senderId.toString());
        req.setContent("hello");

        chatCommandService.sendMessage(req);

        verify(chatStorageStrategy).persistNewMessage(any(ChatMessage.class), eq(List.of(receiverId.toString())));
        verify(realtimeBroadcastService).broadcast(eq(conversationId.toString()), any());
        verify(inboxBroadcastService, never()).notifyMessageCreated(any(), any(), any());
    }
}
