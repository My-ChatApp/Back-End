package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.outbox.OutboxService;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;

import java.util.List;

public class ValkeyAsyncDynamoStorageStrategy extends AbstractValkeyStorageStrategy {

    public ValkeyAsyncDynamoStorageStrategy(
            ChatSpaceRepository chatSpaceRepository,
            ChatMessageRepository chatMessageRepository,
            OutboxService outboxService) {
        super(chatSpaceRepository, chatMessageRepository, outboxService);
    }

    @Override
    public void persistNewMessage(ChatMessage message, List<String> receiverIds) {
        chatSpaceRepository.appendMessage(message);
        publishMessageCreated(message, receiverIds);
    }
}
