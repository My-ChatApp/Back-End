package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class DynamoDbOnlyStorageStrategy implements ChatStorageStrategy {

    private final ChatMessageRepository chatMessageRepository;

    @Override
    public void persistNewMessage(ChatMessage message, List<String> receiverIds) {
        chatMessageRepository.save(message);
    }

    @Override
    public MessagesPageResponse getMessages(
            String conversationId,
            String userId,
            int limit,
            String beforeMessageId) {
        List<ChatMessage> legacy = new ArrayList<>(
                chatMessageRepository.findByConversationId(conversationId, limit));
        Collections.reverse(legacy);
        return MessagesPageResponse.builder()
                .messages(legacy)
                .loading(false)
                .hasMore(legacy.size() >= limit)
                .build();
    }
}
