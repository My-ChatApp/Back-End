package iuh.fit.chatservice.event.consumer;

import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.event.payload.ChatMessageCreatedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageReactionsUpdatedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageDeletedEvent;
import iuh.fit.chatservice.event.payload.ChatMessageUpdatedEvent;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.persistence.dynamodb.ChatMessageRepository;
import iuh.fit.chatservice.service.ChatInboxBroadcastService;
import iuh.fit.chatservice.service.MessageAttachmentMapper;
import iuh.fit.chatservice.repository.ConversationMemberRepository;
import iuh.fit.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@RabbitListener(queues = "${rabbitmq.internal.persist-queue:chat.persist.queue}")
public class ChatPersistConsumer {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatInboxBroadcastService inboxBroadcastService;

    @RabbitHandler
    @Transactional
    public void handleMessageCreated(ChatMessageCreatedEvent event) {
        if (event == null || event.getMessageId() == null) {
            return;
        }
        if (chatMessageRepository.existsByMessageId(event.getMessageId())) {
            log.info("[WriteConsumer] Skip persist (already in DynamoDB): messageId={} conversationId={}",
                    event.getMessageId(), event.getConversationId());
            return;
        }

        if (event.getType() == MessageType.FILE
                && (event.getAttachments() == null || event.getAttachments().isEmpty())) {
            log.warn(
                    "[WriteConsumer] FILE event has no attachments (check RabbitMQ JSON): messageId={} conversationId={}",
                    event.getMessageId(),
                    event.getConversationId());
        }

        ChatMessage message = toChatMessage(event);
        try {
            chatMessageRepository.save(message);
        } catch (Exception e) {
            log.error(
                    "[WriteConsumer] DynamoDB save failed: messageId={} type={} attachmentCount={}",
                    event.getMessageId(),
                    event.getType(),
                    event.getAttachmentCount(),
                    e);
            throw e;
        }

        UUID conversationId = UUID.fromString(event.getConversationId());
        UUID senderId = UUID.fromString(event.getSenderId());

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

        conversation.setLastMessageId(UUID.fromString(event.getMessageId()));
        conversation.setLastMessageSenderId(senderId);
        conversation.setLastMessageType(event.getType().name());
        conversation.setLastMessageAt(event.getCreatedAt());
        conversation.setLastMessagePreview(
                MessageAttachmentMapper.buildLastMessagePreview(
                        event.getType(), event.getContent(), event.getAttachments()));
        conversationRepository.save(conversation);

        conversationMemberRepository.incrementUnreadForOthers(conversationId, senderId);

        broadcastInboxAfterPersist(conversationId, message, event.getReceiverIds());

        if (event.getType() == MessageType.FILE) {
            log.info(
                    "[WriteConsumer] Persist success FILE: messageId={} conversationId={} attachmentCount={}",
                    event.getMessageId(),
                    event.getConversationId(),
                    event.getAttachmentCount());
        } else {
            log.info("[WriteConsumer] Persist success: messageId={} conversationId={} senderId={} (DynamoDB + PostgreSQL)",
                    event.getMessageId(), event.getConversationId(), event.getSenderId());
        }
    }

    private void broadcastInboxAfterPersist(
            UUID conversationId, ChatMessage message, List<String> receiverIds) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        Conversation fresh = conversationRepository.findByIdWithMembers(conversationId)
                .orElse(null);
        if (fresh == null) {
            return;
        }
        for (String receiverId : receiverIds) {
            inboxBroadcastService.notifyMessageCreated(receiverId, fresh, message);
        }
    }

    @RabbitHandler
    @Transactional
    public void handleMessageReactionsUpdated(ChatMessageReactionsUpdatedEvent event) {
        if (event == null || event.getMessageId() == null) {
            return;
        }
        try {
            chatMessageRepository.updateMessageReactions(
                    event.getMessageId(),
                    event.getConversationId(),
                    event.getReactions(),
                    event.getReactionCount());
            log.info(
                    "[WriteConsumer] Reactions update success: messageId={} conversationId={} count={}",
                    event.getMessageId(),
                    event.getConversationId(),
                    event.getReactionCount());
        } catch (RuntimeException e) {
            log.warn(
                    "[WriteConsumer] Reactions update skipped: messageId={} — {}",
                    event.getMessageId(),
                    e.getMessage());
        }
    }

    @RabbitHandler
    @Transactional
    public void handleMessageUpdated(ChatMessageUpdatedEvent event) {
        if (event == null || event.getMessageId() == null) {
            return;
        }
        ChatMessage message = ChatMessage.builder()
                .messageId(event.getMessageId())
                .conversationId(event.getConversationId())
                .senderId(event.getSenderId())
                .type(event.getType())
                .content(event.getContent())
                .edited(true)
                .editedAt(event.getEditedAt())
                .createdAt(event.getCreatedAt())
                .build();
        chatMessageRepository.updateMessageContent(message);
        log.info("[WriteConsumer] Update success: messageId={} conversationId={} (DynamoDB)",
                event.getMessageId(), event.getConversationId());
    }

    @RabbitHandler
    @Transactional
    public void handleMessageDeleted(ChatMessageDeletedEvent event) {
        if (event == null || event.getMessageId() == null) {
            return;
        }
        ChatMessage message = ChatMessage.builder()
                .messageId(event.getMessageId())
                .conversationId(event.getConversationId())
                .senderId(event.getSenderId())
                .type(event.getType())
                .deleted(true)
                .deletedAt(event.getDeletedAt())
                .createdAt(event.getCreatedAt())
                .build();
        chatMessageRepository.updateMessageDeleted(message);

        UUID conversationId = UUID.fromString(event.getConversationId());
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            if (conv.getLastMessageId() != null
                    && conv.getLastMessageId().toString().equals(event.getMessageId())) {
                conv.setLastMessagePreview("Tin nhắn đã bị xóa");
                conversationRepository.save(conv);
            }
        });

        log.info("[WriteConsumer] Delete success: messageId={} conversationId={} (DynamoDB)",
                event.getMessageId(), event.getConversationId());
    }

    private static ChatMessage toChatMessage(ChatMessageCreatedEvent event) {
        return ChatMessage.builder()
                .messageId(event.getMessageId())
                .conversationId(event.getConversationId())
                .senderId(event.getSenderId())
                .type(event.getType())
                .content(event.getContent())
                .replyToMessageId(event.getReplyToMessageId())
                .replyToPreview(event.getReplyToPreview())
                .edited(event.isEdited())
                .deleted(event.isDeleted())
                .createdAt(event.getCreatedAt())
                .attachmentCount(event.getAttachmentCount())
                .reactionCount(event.getReactionCount())
                .attachments(event.getAttachments() != null ? event.getAttachments() : List.of())
                .build();
    }

}
