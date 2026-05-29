package iuh.fit.chatservice.persistence.dynamodb;

import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.model.ChatMessage;
import iuh.fit.chatservice.model.MessageAttachmentDto;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatMessageItemFactoryTest {

    private final ChatMessageItemFactory factory = new ChatMessageItemFactory();

    @Test
    void keysFor_buildsTimelineAndMetaKeys() {
        ChatMessage message = textMessage("msg-1", "conv-1", "hello");

        ChatMessageDynamoKeys keys = factory.keysFor(message);

        assertEquals("CONV#conv-1", keys.timelinePk());
        assertEquals("MSG#2025-01-01T00:00:00Z#msg-1", keys.timelineSk());
        assertEquals("MSG#msg-1", keys.metaPk());
        assertEquals("META", keys.metaSk());
        assertEquals("2025-01-01T00:00:00Z", keys.createdAtIso());
    }

    @Test
    void roundTrip_textMessage() {
        ChatMessage original = textMessage("msg-2", "conv-2", "Hi there");

        ChatMessageDynamoKeys keys = factory.keysFor(original);
        Map<String, AttributeValue> item = factory.createMessageItem(original, keys);
        ChatMessage restored = factory.fromTimelineItem(item);

        assertEquals(original.getMessageId(), restored.getMessageId());
        assertEquals(original.getConversationId(), restored.getConversationId());
        assertEquals(original.getSenderId(), restored.getSenderId());
        assertEquals(MessageType.TEXT, restored.getType());
        assertEquals("Hi there", restored.getContent());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
        assertEquals(0, restored.getAttachmentCount());
        assertEquals(true, restored.getAttachments().isEmpty());
    }

    @Test
    void roundTrip_fileMessageWithAttachments() {
        MessageAttachmentDto attachment = MessageAttachmentDto.builder()
                .attachmentId("att-1")
                .fileType("IMAGE")
                .mimeType("image/jpeg")
                .url("https://cdn.example/photo.jpg")
                .s3Key("chat/inbox/u1/photo.jpg")
                .fileName("photo.jpg")
                .size(1024L)
                .width(800)
                .height(600)
                .build();

        ChatMessage original = ChatMessage.builder()
                .messageId("msg-3")
                .conversationId("conv-3")
                .senderId("user-1")
                .type(MessageType.FILE)
                .content("caption")
                .attachments(List.of(attachment))
                .attachmentCount(1)
                .createdAt(Instant.parse("2025-06-01T12:00:00Z"))
                .build();

        ChatMessageDynamoKeys keys = factory.keysFor(original);
        Map<String, AttributeValue> item = factory.createMessageItem(original, keys);
        ChatMessage restored = factory.fromTimelineItem(item);

        assertEquals(MessageType.FILE, restored.getType());
        assertEquals(1, restored.getAttachments().size());
        MessageAttachmentDto restoredAtt = restored.getAttachments().getFirst();
        assertEquals("att-1", restoredAtt.getAttachmentId());
        assertEquals("IMAGE", restoredAtt.getFileType());
        assertEquals(1024L, restoredAtt.getSize());
        assertEquals(800, restoredAtt.getWidth());
        assertEquals(600, restoredAtt.getHeight());
        assertNull(restoredAtt.getDuration());
    }

    @Test
    void createMetaItem_referencesTimelineKeys() {
        ChatMessage message = textMessage("msg-4", "conv-4", "meta test");
        ChatMessageDynamoKeys keys = factory.keysFor(message);

        Map<String, AttributeValue> meta = factory.createMetaItem(message, keys);

        assertEquals("MSG#msg-4", meta.get("PK").s());
        assertEquals("META", meta.get("SK").s());
        assertEquals("MESSAGE_META", meta.get("entityType").s());
        assertEquals(keys.timelinePk(), meta.get("timelinePk").s());
        assertEquals(keys.timelineSk(), meta.get("timelineSk").s());
    }

    private static ChatMessage textMessage(String messageId, String conversationId, String content) {
        return ChatMessage.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .senderId("user-1")
                .type(MessageType.TEXT)
                .content(content)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();
    }
}
