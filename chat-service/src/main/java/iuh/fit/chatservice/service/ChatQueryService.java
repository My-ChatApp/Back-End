package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.response.MessageSearchResult;
import iuh.fit.chatservice.dto.response.MessagesPageResponse;
import iuh.fit.chatservice.exception.ForbiddenException;
import iuh.fit.chatservice.exception.ResourceNotFoundException;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import iuh.fit.chatservice.storage.ChatStorageStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatQueryService {

    private final ChatStorageStrategy chatStorageStrategy;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectProvider<ChatSpaceRepository> chatSpaceRepositoryProvider;

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

    @Transactional(readOnly = true)
    public MessagesPageResponse getMessagesAround(
            String conversationId, String userId, String anchorMessageId, int limit) {

        UUID convUuid = UUID.fromString(conversationId);
        UUID userUuid = UUID.fromString(userId);

        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(convUuid, userUuid)) {
            throw new ForbiddenException("Bạn không phải thành viên của hội thoại này");
        }

        ChatMessage anchor = chatMessageRepository.findByMessageId(anchorMessageId)
                .filter(m -> conversationId.equals(m.getConversationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn"));

        int pageSize = limit > 0 ? limit : 20;
        int half = Math.max(pageSize / 2, 1);
        Instant anchorCreatedAt = anchor.getCreatedAt();

        List<ChatMessage> older = chatMessageRepository.findByConversationIdBefore(
                conversationId, anchorCreatedAt, anchorMessageId, half);
        List<ChatMessage> newer = chatMessageRepository.findByConversationIdAfter(
                conversationId, anchorCreatedAt, anchorMessageId, half);

        Map<String, ChatMessage> byId = new LinkedHashMap<>();
        for (ChatMessage m : older) {
            byId.put(m.getMessageId(), m);
        }
        byId.put(anchor.getMessageId(), anchor);
        for (ChatMessage m : newer) {
            byId.put(m.getMessageId(), m);
        }

        List<ChatMessage> window = new ArrayList<>(byId.values());
        window.sort(Comparator
                .comparing(ChatMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ChatMessage::getMessageId));

        ChatSpaceRepository chatSpace = chatSpaceRepositoryProvider.getIfAvailable();
        if (chatSpace != null && !window.isEmpty()) {
            chatSpace.appendMessagesBatch(window);
        }

        ChatMessage oldest = window.get(0);
        boolean hasMoreOlder = !chatMessageRepository.findByConversationIdBefore(
                conversationId,
                oldest.getCreatedAt(),
                oldest.getMessageId(),
                1).isEmpty();

        return MessagesPageResponse.builder()
                .messages(window)
                .loading(false)
                .hasMore(hasMoreOlder)
                .build();
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
