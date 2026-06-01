package iuh.fit.chatservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.chatservice.config.ChatOutboxProperties;
import iuh.fit.chatservice.entity.OutboxEvent;
import iuh.fit.chatservice.entity.enums.OutboxEventStatus;
import iuh.fit.chatservice.entity.enums.OutboxEventType;
import iuh.fit.chatservice.event.payload.ChatMessageCreatedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageReactionsUpdatedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageDeletedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageUpdatedEvent;
import iuh.fit.chatservice.event.payload.ConversationHistoryLoadRequestedEvent;
import iuh.fit.chatservice.event.publisher.ChatInternalEventPublisher;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.repository.OutboxEventRepository;
import iuh.fit.chatservice.storage.ChatMessageEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ChatInternalEventPublisher internalEventPublisher;
    private final ObjectMapper chatSpaceObjectMapper;
    private final ChatOutboxProperties outboxProperties;

    @Value("${rabbitmq.internal.persist-routing-key:chat.message.created}")
    private String persistRoutingKey;

    @Value("${rabbitmq.internal.updated-routing-key:chat.message.updated}")
    private String updatedRoutingKey;

    @Value("${rabbitmq.internal.deleted-routing-key:chat.message.deleted}")
    private String deletedRoutingKey;

    @Value("${rabbitmq.internal.reactions-routing-key:chat.message.reactions.updated}")
    private String reactionsRoutingKey;

    @Value("${rabbitmq.internal.hydrate-routing-key:chat.history.load}")
    private String hydrateRoutingKey;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueMessageCreated(ChatMessage message, List<String> receiverIds) {
        ChatMessageCreatedEvent event = ChatMessageEventMapper.toCreatedEvent(message, receiverIds);
        if (!outboxProperties.isEnabled()) {
            internalEventPublisher.publishMessageCreated(event);
            return;
        }
        enqueue(event.getMessageId(), OutboxEventType.MESSAGE_CREATED, persistRoutingKey, event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueMessageUpdated(ChatMessageUpdatedEvent event) {
        if (!outboxProperties.isEnabled()) {
            internalEventPublisher.publishMessageUpdated(event);
            return;
        }
        enqueue(event.getMessageId(), OutboxEventType.MESSAGE_UPDATED, updatedRoutingKey, event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueMessageDeleted(ChatMessageDeletedEvent event) {
        if (!outboxProperties.isEnabled()) {
            internalEventPublisher.publishMessageDeleted(event);
            return;
        }
        enqueue(event.getMessageId(), OutboxEventType.MESSAGE_DELETED, deletedRoutingKey, event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueMessageReactionsUpdated(ChatMessageReactionsUpdatedEvent event) {
        if (!outboxProperties.isEnabled()) {
            internalEventPublisher.publishMessageReactionsUpdated(event);
            return;
        }
        enqueue(event.getMessageId(), OutboxEventType.MESSAGE_REACTIONS_UPDATED, reactionsRoutingKey, event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueHistoryLoadRequested(ConversationHistoryLoadRequestedEvent event) {
        if (!outboxProperties.isEnabled()) {
            internalEventPublisher.publishHistoryLoadRequested(event);
            return;
        }
        String aggregateId = event.getConversationId() + ":" + event.getBeforeMessageId();
        enqueue(aggregateId, OutboxEventType.HISTORY_LOAD_REQUESTED, hydrateRoutingKey, event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void tryPublish(UUID outboxId) {
        OutboxEvent row = outboxEventRepository.findById(outboxId).orElse(null);
        if (row == null || row.getStatus() == OutboxEventStatus.PUBLISHED) {
            return;
        }
        publishRow(row);
    }

    @Transactional
    public void processPendingBatch() {
        if (!outboxProperties.isEnabled()) {
            return;
        }
        List<OutboxEvent> batch = outboxEventRepository.findPendingForPublish(outboxProperties.getBatchSize());
        for (OutboxEvent row : batch) {
            try {
                publishRow(row);
            } catch (Exception e) {
                log.warn("[Outbox] Poller failed for id={}: {}", row.getId(), e.getMessage());
            }
        }
    }

    private void enqueue(String aggregateId, OutboxEventType type, String routingKey, Object payload) {
        try {
            OutboxEvent row = createPendingRow(aggregateId, type, routingKey, payload);
            outboxEventRepository.save(row);
            publishRow(row);
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("[Outbox] Duplicate pending {} aggregateId={}", type, aggregateId);
            outboxEventRepository
                    .findByAggregateIdAndEventTypeAndStatus(aggregateId, type, OutboxEventStatus.PENDING)
                    .ifPresent(this::publishRow);
        }
    }

    private OutboxEvent createPendingRow(
            String aggregateId, OutboxEventType type, String routingKey, Object payload) {
        Instant now = Instant.now();
        return OutboxEvent.builder()
                .aggregateId(aggregateId)
                .eventType(type)
                .routingKey(routingKey)
                .payload(serialize(payload))
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(now)
                .createdAt(now)
                .build();
    }

    private void publishRow(OutboxEvent row) {
        if (row.getStatus() == OutboxEventStatus.PUBLISHED) {
            return;
        }
        try {
            dispatchToRabbit(row);
            row.setStatus(OutboxEventStatus.PUBLISHED);
            row.setPublishedAt(Instant.now());
            row.setLastError(null);
        } catch (Exception e) {
            handlePublishFailure(row, e);
        }
        outboxEventRepository.save(row);
    }

    private void dispatchToRabbit(OutboxEvent row) throws JsonProcessingException {
        switch (row.getEventType()) {
            case MESSAGE_CREATED -> internalEventPublisher.publishMessageCreated(
                    chatSpaceObjectMapper.readValue(row.getPayload(), ChatMessageCreatedEvent.class));
            case MESSAGE_UPDATED -> internalEventPublisher.publishMessageUpdated(
                    chatSpaceObjectMapper.readValue(row.getPayload(), ChatMessageUpdatedEvent.class));
            case MESSAGE_DELETED -> internalEventPublisher.publishMessageDeleted(
                    chatSpaceObjectMapper.readValue(row.getPayload(), ChatMessageDeletedEvent.class));
            case MESSAGE_REACTIONS_UPDATED -> internalEventPublisher.publishMessageReactionsUpdated(
                    chatSpaceObjectMapper.readValue(row.getPayload(), ChatMessageReactionsUpdatedEvent.class));
            case HISTORY_LOAD_REQUESTED -> internalEventPublisher.publishHistoryLoadRequested(
                    chatSpaceObjectMapper.readValue(row.getPayload(), ConversationHistoryLoadRequestedEvent.class));
            default -> throw new IllegalStateException("Unknown outbox event type: " + row.getEventType());
        }
    }

    private void handlePublishFailure(OutboxEvent row, Exception e) {
        int nextRetry = row.getRetryCount() + 1;
        row.setRetryCount(nextRetry);
        row.setLastError(truncateError(e.getMessage()));
        if (nextRetry >= outboxProperties.getMaxPublishAttempts()) {
            row.setStatus(OutboxEventStatus.FAILED);
            log.error(
                    "[Outbox] Publish exhausted type={} aggregateId={} attempts={}: {}",
                    row.getEventType(),
                    row.getAggregateId(),
                    nextRetry,
                    e.getMessage());
        } else {
            row.setStatus(OutboxEventStatus.PENDING);
            row.setNextRetryAt(Instant.now().plusMillis(calculateBackoffMs(nextRetry)));
            log.warn(
                    "[Outbox] Publish failed type={} aggregateId={} retry={}/{}: {}",
                    row.getEventType(),
                    row.getAggregateId(),
                    nextRetry,
                    outboxProperties.getMaxPublishAttempts(),
                    e.getMessage());
        }
    }

    private long calculateBackoffMs(int retryCount) {
        double delay = outboxProperties.getRetryInitialIntervalMs()
                * Math.pow(outboxProperties.getRetryMultiplier(), Math.max(0, retryCount - 1));
        return (long) Math.min(delay, outboxProperties.getRetryMaxIntervalMs());
    }

    private String serialize(Object payload) {
        try {
            return chatSpaceObjectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private static String truncateError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
