package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.TypingEventRequest;
import iuh.fit.chatservice.event.payload.ChatRealtimeEnvelope;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatTypingService {

    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatRealtimeBroadcastService realtimeBroadcastService;

    @Transactional(readOnly = true)
    public void handleTyping(TypingEventRequest request) {
        if (request == null
                || request.getConversationId() == null
                || request.getUserId() == null) {
            return;
        }

        UUID conversationId = UUID.fromString(request.getConversationId());
        UUID userId = UUID.fromString(request.getUserId());

        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, userId)) {
            throw new RuntimeException("Not a member of this conversation");
        }

        realtimeBroadcastService.broadcast(
                conversationId.toString(),
                ChatRealtimeEnvelope.builder()
                        .eventType(ChatRealtimeEnvelope.EventType.TYPING)
                        .conversationId(conversationId.toString())
                        .userId(userId.toString())
                        .typing(request.isTyping())
                        .build());
    }
}
