package iuh.fit.chatservice.space;

import iuh.fit.chatservice.model.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface ChatSpaceRepository {

    void appendMessage(ChatMessage message);

    void appendMessagesBatch(List<ChatMessage> messages);

    List<ChatMessage> getRecent(String conversationId, int limit);

    List<ChatMessage> getBefore(String conversationId, String beforeMessageId, int limit);

    Optional<ChatMessage> getMessage(String messageId);

    void updateMessage(ChatMessage message);

    long countInTimeline(String conversationId);

    boolean tryAcquireHydrateLock(String conversationId, int ttlSeconds);

    void releaseHydrateLock(String conversationId);
}
