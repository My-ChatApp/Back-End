package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.model.ChatMessage;

import java.util.List;

public interface ChatStorageStrategy {

    void persistNewMessage(ChatMessage message, List<String> receiverIds);

    MessagesPageResponse getMessages(
            String conversationId,
            String userId,
            int limit,
            String beforeMessageId);
}
