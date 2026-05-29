package iuh.fit.chatservice.config;

import iuh.fit.chatservice.event.publisher.ChatInternalEventPublisher;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import iuh.fit.chatservice.storage.ChatStorageStrategy;
import iuh.fit.chatservice.storage.DynamoDbOnlyStorageStrategy;
import iuh.fit.chatservice.storage.ValkeyAsyncDynamoStorageStrategy;
import iuh.fit.chatservice.storage.ValkeyDualWriteStorageStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatStorageStrategyConfig {

    @Bean
    public ChatStorageStrategy chatStorageStrategy(
            ChatSpaceProperties chatSpaceProperties,
            ChatMessageRepository chatMessageRepository,
            ObjectProvider<ChatSpaceRepository> chatSpaceRepositoryProvider,
            ChatInternalEventPublisher internalEventPublisher) {
        if (!chatSpaceProperties.isEnabled()) {
            return new DynamoDbOnlyStorageStrategy(chatMessageRepository);
        }
        ChatSpaceRepository chatSpaceRepository = chatSpaceRepositoryProvider.getObject();
        if (chatSpaceProperties.isDualWriteDynamodb()) {
            return new ValkeyDualWriteStorageStrategy(
                    chatSpaceRepository, chatMessageRepository, internalEventPublisher);
        }
        return new ValkeyAsyncDynamoStorageStrategy(
                chatSpaceRepository, chatMessageRepository, internalEventPublisher);
    }
}
