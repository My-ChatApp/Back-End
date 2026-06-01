package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.SendMessageRequest;
import iuh.fit.chatservice.dto.request.UpdateMessageRequest;
import iuh.fit.chatservice.dto.response.MessageSearchResult;
import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Facade giữ tương thích controller cũ — delegate sang command/query space-based.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatCommandService chatCommandService;
    private final ChatQueryService chatQueryService;
    private final MessageReactionService messageReactionService;

    public ChatMessage sendMessage(SendMessageRequest req) {
        return chatCommandService.sendMessage(req);
    }

    public ChatMessage updateMessage(String conversationId, String messageId, String userId, UpdateMessageRequest req) {
        return chatCommandService.updateMessage(conversationId, messageId, userId, req);
    }

    public ChatMessage deleteMessage(String conversationId, String messageId, String userId) {
        return chatCommandService.deleteMessage(conversationId, messageId, userId);
    }

    public MessagesPageResponse getMessages(String conversationId, String userId, int limit, String before) {
        return chatQueryService.getMessages(conversationId, userId, limit, before);
    }

    public java.util.List<MessageSearchResult> searchMessages(
            String conversationId, String userId, String query, int limit) {
        return chatQueryService.searchMessages(conversationId, userId, query, limit);
    }

    public MessagesPageResponse getMessagesAround(
            String conversationId, String userId, String anchorMessageId, int limit) {
        return chatQueryService.getMessagesAround(conversationId, userId, anchorMessageId, limit);
    }

    public ChatMessage setMessageReaction(
            String conversationId, String messageId, String userId, String reactionType) {
        return messageReactionService.setReaction(conversationId, messageId, userId, reactionType);
    }

    public ChatMessage removeMessageReaction(String conversationId, String messageId, String userId) {
        return messageReactionService.removeReaction(conversationId, messageId, userId);
    }
}
