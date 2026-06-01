package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.SendMessageRequest;
import iuh.fit.chatservice.dto.request.UpdateMessageRequest;
import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.event.payload.ChatMessageDeletedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageUpdatedEvent;
import iuh.fit.chatservice.event.payload.ChatRealtimeEnvelope;
import iuh.fit.chatservice.outbox.OutboxService;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.model.MessageAttachmentDto;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.repository.ConversationRepository;
import iuh.fit.chatservice.space.ChatSpaceRepository;
import iuh.fit.chatservice.storage.ChatStorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCommandService {

    private final ChatStorageStrategy chatStorageStrategy;
    private final ChatSpaceRepository chatSpaceRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OutboxService outboxService;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatRealtimeBroadcastService realtimeBroadcastService;
    private final MessageAttachmentMapper messageAttachmentMapper;

    @Transactional(readOnly = true)
    public ChatMessage sendMessage(SendMessageRequest req) {
        UUID conversationId = UUID.fromString(req.getConversationId());
        UUID senderId = UUID.fromString(req.getSenderId());

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(
                conversationId, senderId)) {
            throw new RuntimeException("Not a member of this conversation");
        }

        List<MessageAttachmentDto> attachments = messageAttachmentMapper.toDtoList(req.getAttachments());
        MessageType type = messageAttachmentMapper.resolveType(req, attachments);
        messageAttachmentMapper.validate(type, attachments, req.getContent());

        if (type == MessageType.FILE) {
            String firstKey = attachments.isEmpty() ? null : attachments.get(0).getS3Key();
            log.debug(
                    "sendMessage FILE conversationId={} attachmentCount={} firstS3Key={}",
                    req.getConversationId(),
                    attachments.size(),
                    firstKey);
        }

        Instant now = Instant.now();
        String messageId = UUID.randomUUID().toString();

        ChatMessage message = ChatMessage.builder()
                .messageId(messageId)
                .conversationId(conversationId.toString())
                .senderId(senderId.toString())
                .type(type)
                .content(req.getContent())
                .attachments(attachments)
                .attachmentCount(attachments.size())
                .createdAt(now)
                .build();

        List<String> receiverIds = resolveReceiverIds(conversationId, senderId);

        chatStorageStrategy.persistNewMessage(message, receiverIds);

        realtimeBroadcastService.broadcast(
                conversationId.toString(),
                ChatRealtimeEnvelope.builder()
                        .eventType(ChatRealtimeEnvelope.EventType.MESSAGE_CREATED)
                        .message(message)
                        .build());

        return message;
    }

    @Transactional(readOnly = true)
    public ChatMessage updateMessage(String conversationId, String messageId, String userId, UpdateMessageRequest req) {
        UUID convUuid = UUID.fromString(conversationId);
        UUID userUuid = UUID.fromString(userId);

        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(convUuid, userUuid)) {
            throw new RuntimeException("Not a member of this conversation");
        }

        ChatMessage existing = chatSpaceRepository.getMessage(messageId)
                .or(() -> chatMessageRepository.findByMessageId(messageId))
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!existing.getConversationId().equals(conversationId)) {
            throw new RuntimeException("Message does not belong to conversation");
        }
        if (!existing.getSenderId().equals(userId)) {
            throw new RuntimeException("Only sender can edit message");
        }
        if (existing.isDeleted()) {
            throw new RuntimeException("Cannot edit deleted message");
        }
        if (existing.getType() != MessageType.TEXT) {
            throw new RuntimeException("Only text messages can be edited");
        }
        messageAttachmentMapper.validate(MessageType.TEXT, List.of(), req.getContent());

        Instant now = Instant.now();
        existing.setContent(req.getContent().trim());
        existing.setEdited(true);
        existing.setEditedAt(now);
        chatSpaceRepository.updateMessage(existing);

        outboxService.enqueueMessageUpdated(ChatMessageUpdatedEvent.builder()
                .messageId(existing.getMessageId())
                .conversationId(existing.getConversationId())
                .senderId(existing.getSenderId())
                .type(existing.getType())
                .content(existing.getContent())
                .edited(true)
                .editedAt(now)
                .createdAt(existing.getCreatedAt())
                .build());

        realtimeBroadcastService.broadcast(
                conversationId,
                ChatRealtimeEnvelope.builder()
                        .eventType(ChatRealtimeEnvelope.EventType.MESSAGE_UPDATED)
                        .message(existing)
                        .build());

        return existing;
    }

    @Transactional(readOnly = true)
    public ChatMessage deleteMessage(String conversationId, String messageId, String userId) {
        UUID convUuid = UUID.fromString(conversationId);
        UUID userUuid = UUID.fromString(userId);

        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserIdAndDeletedFalse(convUuid, userUuid)) {
            throw new RuntimeException("Not a member of this conversation");
        }

        ChatMessage existing = chatSpaceRepository.getMessage(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!existing.getConversationId().equals(conversationId)) {
            throw new RuntimeException("Message does not belong to conversation");
        }
        if (!existing.getSenderId().equals(userId)) {
            throw new RuntimeException("Only sender can delete message");
        }
        if (existing.isDeleted()) {
            return existing;
        }

        Instant now = Instant.now();
        existing.setDeleted(true);
        existing.setDeletedAt(now);
        existing.setContent("");
        existing.setAttachments(List.of());
        existing.setAttachmentCount(0);
        existing.setReactions(List.of());
        existing.setReactionCount(0);
        chatSpaceRepository.updateMessage(existing);

        outboxService.enqueueMessageDeleted(ChatMessageDeletedEvent.builder()
                .messageId(existing.getMessageId())
                .conversationId(existing.getConversationId())
                .senderId(existing.getSenderId())
                .type(existing.getType())
                .deleted(true)
                .deletedAt(now)
                .createdAt(existing.getCreatedAt())
                .build());

        realtimeBroadcastService.broadcast(
                conversationId,
                ChatRealtimeEnvelope.builder()
                        .eventType(ChatRealtimeEnvelope.EventType.MESSAGE_DELETED)
                        .message(existing)
                        .build());

        return existing;
    }

    private List<String> resolveReceiverIds(UUID conversationId, UUID senderId) {
        return conversationMemberRepository
                .findById_ConversationIdAndDeletedFalse(conversationId)
                .stream()
                .map(member -> member.getUserId().toString())
                .filter(id -> !id.equals(senderId.toString()))
                .toList();
    }
}
