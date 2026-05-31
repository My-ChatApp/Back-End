package iuh.fit.chatservice.storage;

import iuh.fit.chatservice.event.payload.ChatMessageCreatedEvent;
import iuh.fit.chatservice.model.ChatMessage;

import java.util.List;

public final class ChatMessageEventMapper {

    private ChatMessageEventMapper() {
    }

    public static ChatMessageCreatedEvent toCreatedEvent(ChatMessage message, List<String> receiverIds) {
        return ChatMessageCreatedEvent.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .type(message.getType())
                .content(message.getContent())
                .replyToMessageId(message.getReplyToMessageId())
                .replyToPreview(message.getReplyToPreview())
                .edited(message.isEdited())
                .deleted(message.isDeleted())
                .createdAt(message.getCreatedAt())
                .attachmentCount(message.getAttachmentCount())
                .reactionCount(message.getReactionCount())
                .attachments(message.getAttachments())
                .receiverIds(receiverIds)
                .build();
    }
}
