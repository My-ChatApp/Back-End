package iuh.fit.chatservice.event.consumer;

import iuh.fit.chatservice.config.ChatSpaceProperties;
import iuh.fit.chatservice.event.payload.ConversationHistoryLoadRequestedEvent;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHydrationConsumer {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSpaceRepository chatSpaceRepository;
    private final ChatSpaceProperties chatSpaceProperties;

    @RabbitListener(queues = "${rabbitmq.internal.hydrate-queue:chat.hydrate.queue}")
    public void handleHistoryLoad(ConversationHistoryLoadRequestedEvent event) {
        if (event == null || event.getConversationId() == null) {
            return;
        }
        String conversationId = event.getConversationId();
        try {
            int batch = event.getLimit() > 0
                    ? Math.min(event.getLimit(), chatSpaceProperties.getHydrateBatchSize())
                    : chatSpaceProperties.getHydrateBatchSize();

            List<ChatMessage> older = chatMessageRepository.findByConversationIdBefore(
                    conversationId,
                    event.getBeforeCreatedAt(),
                    event.getBeforeMessageId(),
                    batch);

            if (!older.isEmpty()) {
                chatSpaceRepository.appendMessagesBatch(older);
                log.info(
                        "[ReadConsumer] Hydrate success: loaded {} message(s) from DynamoDB into Valkey — conversationId={} beforeMessageId={}",
                        older.size(), conversationId, event.getBeforeMessageId());
            } else {
                log.info(
                        "[ReadConsumer] Hydrate success: no older messages in DynamoDB — conversationId={} beforeMessageId={}",
                        conversationId, event.getBeforeMessageId());
            }
        } catch (Exception e) {
            log.error("[ReadConsumer] Hydrate failed: conversationId={} — {}",
                    conversationId, e.getMessage(), e);
            throw e;
        } finally {
            chatSpaceRepository.releaseHydrateLock(conversationId);
        }
    }
}
