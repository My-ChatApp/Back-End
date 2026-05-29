package iuh.fit.chatservice.persistence.dynamodb;

import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.model.MessageAttachmentDto;
import iuh.fit.chatservice.model.MessageReactionDto;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChatMessageItemFactory {

    private static final String META_SK = "META";

    public ChatMessageDynamoKeys keysFor(ChatMessage message) {
        String createdAtIso = formatInstant(message.getCreatedAt());
        String timelinePk = conversationPk(message.getConversationId());
        String timelineSk = messageSk(createdAtIso, message.getMessageId());
        String metaPk = "MSG#" + message.getMessageId();
        return new ChatMessageDynamoKeys(timelinePk, timelineSk, metaPk, META_SK, createdAtIso);
    }

    public Map<String, AttributeValue> createMessageItem(ChatMessage message, ChatMessageDynamoKeys keys) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s(keys.timelinePk()).build());
        item.put("SK", AttributeValue.builder().s(keys.timelineSk()).build());
        item.put("entityType", AttributeValue.builder().s("MESSAGE").build());
        item.put("messageId", AttributeValue.builder().s(message.getMessageId()).build());
        item.put("conversationId", AttributeValue.builder().s(message.getConversationId()).build());
        item.put("senderId", AttributeValue.builder().s(message.getSenderId()).build());
        item.put("type", AttributeValue.builder().s(message.getType().name()).build());
        putOptionalString(item, "content", message.getContent());
        putOptionalString(item, "replyToMessageId", message.getReplyToMessageId());
        putOptionalString(item, "replyToPreview", message.getReplyToPreview());
        item.put("edited", AttributeValue.builder().bool(message.isEdited()).build());
        item.put("deleted", AttributeValue.builder().bool(message.isDeleted()).build());
        item.put("createdAt", AttributeValue.builder().s(keys.createdAtIso()).build());
        item.put("attachmentCount", AttributeValue.builder().n(String.valueOf(message.getAttachmentCount())).build());
        item.put("reactionCount", AttributeValue.builder().n(String.valueOf(message.getReactionCount())).build());
        item.put("attachments", AttributeValue.builder().l(toAttachmentAttrs(message.getAttachments())).build());
        item.put("reactions", AttributeValue.builder().l(toReactionAttrs(message.getReactions())).build());
        return item;
    }

    public Map<String, AttributeValue> createMetaItem(ChatMessage message, ChatMessageDynamoKeys keys) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s(keys.metaPk()).build());
        item.put("SK", AttributeValue.builder().s(keys.metaSk()).build());
        item.put("entityType", AttributeValue.builder().s("MESSAGE_META").build());
        item.put("messageId", AttributeValue.builder().s(message.getMessageId()).build());
        item.put("conversationId", AttributeValue.builder().s(message.getConversationId()).build());
        item.put("senderId", AttributeValue.builder().s(message.getSenderId()).build());
        item.put("type", AttributeValue.builder().s(message.getType().name()).build());
        item.put("timelinePk", AttributeValue.builder().s(keys.timelinePk()).build());
        item.put("timelineSk", AttributeValue.builder().s(keys.timelineSk()).build());
        item.put("deleted", AttributeValue.builder().bool(message.isDeleted()).build());
        item.put("createdAt", AttributeValue.builder().s(keys.createdAtIso()).build());
        return item;
    }

    public ChatMessage fromTimelineItem(Map<String, AttributeValue> item) {
        return ChatMessage.builder()
                .messageId(getS(item, "messageId"))
                .conversationId(getS(item, "conversationId"))
                .senderId(getS(item, "senderId"))
                .type(MessageType.valueOf(getS(item, "type")))
                .content(getS(item, "content"))
                .replyToMessageId(getS(item, "replyToMessageId"))
                .replyToPreview(getS(item, "replyToPreview"))
                .edited(getBool(item, "edited"))
                .deleted(getBool(item, "deleted"))
                .createdAt(Instant.parse(getS(item, "createdAt")))
                .attachmentCount(parseInt(item, "attachmentCount"))
                .reactionCount(parseInt(item, "reactionCount"))
                .attachments(fromAttachmentAttrs(item.get("attachments")))
                .reactions(fromReactionAttrs(item.get("reactions")))
                .build();
    }

    public static String conversationPk(String conversationId) {
        return "CONV#" + conversationId;
    }

    public static String messageSk(String createdAtIso, String messageId) {
        return "MSG#" + createdAtIso + "#" + messageId;
    }

    public static String formatInstant(Instant instant) {
        return instant.toString();
    }

    private List<AttributeValue> toAttachmentAttrs(List<MessageAttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream().map(a -> {
            Map<String, AttributeValue> m = new HashMap<>();
            putString(m, "attachmentId", a.getAttachmentId());
            putString(m, "fileType", a.getFileType());
            putString(m, "mimeType", a.getMimeType());
            putString(m, "url", a.getUrl());
            putString(m, "s3Key", a.getS3Key());
            putString(m, "fileName", a.getFileName());
            if (a.getSize() != null) {
                m.put("size", AttributeValue.builder().n(a.getSize().toString()).build());
            }
            if (a.getWidth() != null) {
                m.put("width", AttributeValue.builder().n(a.getWidth().toString()).build());
            }
            if (a.getHeight() != null) {
                m.put("height", AttributeValue.builder().n(a.getHeight().toString()).build());
            }
            if (a.getDuration() != null) {
                m.put("duration", AttributeValue.builder().n(a.getDuration().toString()).build());
            }
            putString(m, "thumbnailUrl", a.getThumbnailUrl());
            return AttributeValue.builder().m(m).build();
        }).collect(Collectors.toList());
    }

    private List<AttributeValue> toReactionAttrs(List<MessageReactionDto> reactions) {
        if (reactions == null || reactions.isEmpty()) {
            return List.of();
        }
        return reactions.stream().map(r -> {
            Map<String, AttributeValue> m = new HashMap<>();
            putString(m, "userId", r.getUserId());
            putString(m, "reactionType", r.getReactionType());
            if (r.getCreatedAt() != null) {
                putString(m, "createdAt", formatInstant(r.getCreatedAt()));
            }
            return AttributeValue.builder().m(m).build();
        }).collect(Collectors.toList());
    }

    private List<MessageAttachmentDto> fromAttachmentAttrs(AttributeValue value) {
        if (value == null || value.l() == null) {
            return List.of();
        }
        return value.l().stream().map(av -> {
            Map<String, AttributeValue> m = av.m();
            return MessageAttachmentDto.builder()
                    .attachmentId(getS(m, "attachmentId"))
                    .fileType(getS(m, "fileType"))
                    .mimeType(getS(m, "mimeType"))
                    .url(getS(m, "url"))
                    .s3Key(getS(m, "s3Key"))
                    .fileName(getS(m, "fileName"))
                    .size(m.containsKey("size") ? Long.parseLong(m.get("size").n()) : null)
                    .width(m.containsKey("width") ? Integer.parseInt(m.get("width").n()) : null)
                    .height(m.containsKey("height") ? Integer.parseInt(m.get("height").n()) : null)
                    .duration(m.containsKey("duration") ? Integer.parseInt(m.get("duration").n()) : null)
                    .thumbnailUrl(getS(m, "thumbnailUrl"))
                    .build();
        }).collect(Collectors.toList());
    }

    private List<MessageReactionDto> fromReactionAttrs(AttributeValue value) {
        if (value == null || value.l() == null) {
            return List.of();
        }
        return value.l().stream().map(av -> {
            Map<String, AttributeValue> m = av.m();
            return MessageReactionDto.builder()
                    .userId(getS(m, "userId"))
                    .reactionType(getS(m, "reactionType"))
                    .createdAt(m.containsKey("createdAt") ? Instant.parse(getS(m, "createdAt")) : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private static void putOptionalString(Map<String, AttributeValue> item, String key, String value) {
        if (value != null) {
            item.put(key, AttributeValue.builder().s(value).build());
        }
    }

    private static void putString(Map<String, AttributeValue> item, String key, String value) {
        if (value != null) {
            item.put(key, AttributeValue.builder().s(value).build());
        }
    }

    private static String getS(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v != null ? v.s() : null;
    }

    private static boolean getBool(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v != null && Boolean.TRUE.equals(v.bool());
    }

    private static int parseInt(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v != null ? Integer.parseInt(v.n()) : 0;
    }
}
