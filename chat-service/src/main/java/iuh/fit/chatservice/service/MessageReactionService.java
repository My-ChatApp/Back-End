package iuh.fit.chatservice.service;

import iuh.fit.chatservice.entity.enums.ReactionType;
import iuh.fit.chatservice.event.payload.ChatMessageReactionsUpdatedEvent;
import iuh.fit.chatservice.event.payload.ChatRealtimeEnvelope;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.model.MessageReactionDto;
import iuh.fit.chatservice.outbox.OutboxService;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageReactionService {

    private final ChatSpaceRepository chatSpaceRepository;
    private final OutboxService outboxService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatRealtimeBroadcastService realtimeBroadcastService;

    @Transactional(readOnly = true)
    public ChatMessage setReaction(
            String conversationId, String messageId, String userId, String reactionTypeRaw) {
        requireMember(conversationId, userId);
        ReactionType reactionType = ReactionType.parse(reactionTypeRaw);

        ChatMessage message = loadMessage(conversationId, messageId);
        List<MessageReactionDto> reactions = new ArrayList<>(
                message.getReactions() != null ? message.getReactions() : List.of());

        Optional<MessageReactionDto> existing = reactions.stream()
                .filter(r -> userId.equals(r.getUserId()))
                .findFirst();

        Instant now = Instant.now();
        if (existing.isPresent()) {
            if (reactionType.name().equals(existing.get().getReactionType())) {
                reactions.remove(existing.get());
            } else {
                existing.get().setReactionType(reactionType.name());
                existing.get().setUpdatedAt(now);
            }
        } else {
            reactions.add(MessageReactionDto.builder()
                    .userId(userId)
                    .reactionType(reactionType.name())
                    .createdAt(now)
                    .build());
        }

        return applyAndBroadcast(message, reactions);
    }

    @Transactional(readOnly = true)
    public ChatMessage removeReaction(String conversationId, String messageId, String userId) {
        requireMember(conversationId, userId);

        ChatMessage message = loadMessage(conversationId, messageId);
        List<MessageReactionDto> reactions = new ArrayList<>(
                message.getReactions() != null ? message.getReactions() : List.of());
        reactions.removeIf(r -> userId.equals(r.getUserId()));

        return applyAndBroadcast(message, reactions);
    }

    private ChatMessage applyAndBroadcast(ChatMessage message, List<MessageReactionDto> reactions) {
        message.setReactions(reactions);
        message.setReactionCount(reactions.size());
        chatSpaceRepository.updateMessage(message);

        outboxService.enqueueMessageReactionsUpdated(ChatMessageReactionsUpdatedEvent.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversationId())
                .reactionCount(reactions.size())
                .reactions(reactions)
                .build());

        realtimeBroadcastService.broadcast(
                message.getConversationId(),
                ChatRealtimeEnvelope.builder()
                        .eventType(ChatRealtimeEnvelope.EventType.MESSAGE_UPDATED)
                        .message(message)
                        .build());

        return message;
    }

    private ChatMessage loadMessage(String conversationId, String messageId) {
        ChatMessage message = chatSpaceRepository.getMessage(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!message.getConversationId().equals(conversationId)) {
            throw new RuntimeException("Message does not belong to conversation");
        }
        if (message.isDeleted()) {
            throw new RuntimeException("Cannot react to deleted message");
        }
        return message;
    }

    private void requireMember(String conversationId, String userId) {
        UUID convUuid = UUID.fromString(conversationId);
        UUID userUuid = UUID.fromString(userId);
        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                convUuid, userUuid)) {
            throw new RuntimeException("Not a member of this conversation");
        }
    }
}
