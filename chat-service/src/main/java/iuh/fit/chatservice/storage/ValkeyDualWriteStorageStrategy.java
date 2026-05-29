package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.event.publisher.ChatInternalEventPublisher;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;

import java.util.List;

public class ValkeyDualWriteStorageStrategy extends AbstractValkeyStorageStrategy {

    public ValkeyDualWriteStorageStrategy(
            ChatSpaceRepository chatSpaceRepository,
            ChatMessageRepository chatMessageRepository,
            ChatInternalEventPublisher internalEventPublisher) {
        super(chatSpaceRepository, chatMessageRepository, internalEventPublisher);
    }

    @Override
    public void persistNewMessage(ChatMessage message, List<String> receiverIds) {
        chatSpaceRepository.appendMessage(message);
        chatMessageRepository.save(message);
        publishMessageCreated(message, receiverIds);
    }
}
