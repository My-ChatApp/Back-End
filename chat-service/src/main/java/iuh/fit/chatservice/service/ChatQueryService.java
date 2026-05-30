package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.response.MessageSearchResult;
import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.exception.ForbiddenException;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.storage.ChatStorageStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatQueryService {

    private final ChatStorageStrategy chatStorageStrategy;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public MessagesPageResponse getMessages(
            String conversationId, String userId, int limit, String beforeMessageId) {

        UUID convUuid = UUID.fromString(conversationId);
        UUID userUuid = UUID.fromString(userId);

        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(convUuid, userUuid)) {
            throw new RuntimeException("Not a member of this conversation");
        }

        return chatStorageStrategy.getMessages(conversationId, userId, limit, beforeMessageId);
    }

    @Transactional(readOnly = true)
    public List<MessageSearchResult> searchMessages(
            String conversationId, String userId, String query, int limit) {

        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm phải có ít nhất 2 ký tự");
        }

        UUID convUuid = UUID.fromString(conversationId);
        UUID userUuid = UUID.fromString(userId);

        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(convUuid, userUuid)) {
            throw new ForbiddenException("Bạn không phải thành viên của hội thoại này");
        }

        return chatMessageRepository.searchByConversationId(conversationId, q, limit).stream()
                .map(this::toSearchResult)
                .toList();
    }

    private MessageSearchResult toSearchResult(ChatMessage message) {
        return MessageSearchResult.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .type(message.getType())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
