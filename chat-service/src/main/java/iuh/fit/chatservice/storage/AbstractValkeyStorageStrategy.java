package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.event.payload.ConversationHistoryLoadRequestedEvent;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.outbox.OutboxService;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractValkeyStorageStrategy implements ChatStorageStrategy {

    protected final ChatSpaceRepository chatSpaceRepository;
    protected final ChatMessageRepository chatMessageRepository;
    protected final OutboxService outboxService;

    @Override
    public MessagesPageResponse getMessages(
            String conversationId,
            String userId,
            int limit,
            String beforeMessageId) {

        List<ChatMessage> fromSpace;
        if (beforeMessageId == null || beforeMessageId.isBlank()) {
            fromSpace = chatSpaceRepository.getRecent(conversationId, limit);
            if (fromSpace.isEmpty()) {
                return coldStartFromDynamo(conversationId, limit);
            }
        } else {
            fromSpace = chatSpaceRepository.getBefore(conversationId, beforeMessageId, limit);
        }

        if (fromSpace.size() >= limit) {
            return MessagesPageResponse.builder()
                    .messages(fromSpace)
                    .loading(false)
                    .hasMore(true)
                    .build();
        }

        boolean needsHydrate = beforeMessageId != null && !beforeMessageId.isBlank();
        if (needsHydrate) {
            Instant beforeCreatedAt = chatSpaceRepository.getMessage(beforeMessageId)
                    .map(ChatMessage::getCreatedAt)
                    .or(() -> chatMessageRepository.findByMessageId(beforeMessageId).map(ChatMessage::getCreatedAt))
                    .orElse(null);

            if (beforeCreatedAt == null) {
                return page(fromSpace, false, false);
            }

            boolean olderExistsInDdb = !chatMessageRepository
                    .findByConversationIdBefore(conversationId, beforeCreatedAt, beforeMessageId, 1)
                    .isEmpty();

            if (!olderExistsInDdb) {
                return page(fromSpace, false, false);
            }

            triggerHydrate(conversationId, userId, beforeMessageId, beforeCreatedAt, limit);
            return page(fromSpace, true, true);
        }

        return page(fromSpace, false, fromSpace.size() >= limit);
    }

    private MessagesPageResponse page(List<ChatMessage> messages, boolean loading, boolean hasMore) {
        return MessagesPageResponse.builder()
                .messages(messages)
                .loading(loading)
                .hasMore(hasMore)
                .build();
    }

    protected void publishMessageCreated(ChatMessage message, List<String> receiverIds) {
        outboxService.enqueueMessageCreated(message, receiverIds);
    }

    private MessagesPageResponse coldStartFromDynamo(String conversationId, int limit) {
        List<ChatMessage> fromDdb = chatMessageRepository.findByConversationId(conversationId, limit);
        List<ChatMessage> chronological = new ArrayList<>();
        if (!fromDdb.isEmpty()) {
            chronological.addAll(fromDdb);
            Collections.reverse(chronological);
            chatSpaceRepository.appendMessagesBatch(chronological);
        }
        return MessagesPageResponse.builder()
                .messages(chronological)
                .loading(false)
                .hasMore(fromDdb.size() >= limit)
                .build();
    }

    private void triggerHydrate(
            String conversationId,
            String userId,
            String beforeMessageId,
            Instant beforeCreatedAt,
            int limit) {
        if (!chatSpaceRepository.tryAcquireHydrateLock(conversationId, 30)) {
            log.debug("Hydrate already in progress for conv {}", conversationId);
            return;
        }
        outboxService.enqueueHistoryLoadRequested(ConversationHistoryLoadRequestedEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .beforeMessageId(beforeMessageId)
                .beforeCreatedAt(beforeCreatedAt)
                .limit(limit)
                .build());
    }
}
