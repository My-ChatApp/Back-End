package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.CreateConversationRequest;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.enums.TypeRoom;
import iuh.fit.chatservice.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatInboxBroadcastService inboxBroadcastService;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void create_privateExisting_returnsExistingWithoutSave() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        Conversation existing = Conversation.builder().id(UUID.randomUUID()).type(TypeRoom.PRIVATE).build();

        when(conversationRepository.findPrivateBetweenUsers(u1, u2)).thenReturn(Optional.of(existing));

        CreateConversationRequest req = new CreateConversationRequest();
        req.setType("PRIVATE");
        req.setMemberIds(List.of(u1.toString(), u2.toString()));

        Conversation result = conversationService.create(req);

        assertEquals(existing.getId(), result.getId());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void create_privateNew_savesConversation() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        when(conversationRepository.findPrivateBetweenUsers(u1, u2)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });

        CreateConversationRequest req = new CreateConversationRequest();
        req.setType("PRIVATE");
        req.setTitle("Chat");
        req.setMemberIds(List.of(u1.toString(), u2.toString()));

        Conversation result = conversationService.create(req);

        verify(conversationRepository).save(any(Conversation.class));
        assertEquals(TypeRoom.PRIVATE, result.getType());
    }
}
