package iuh.fit.chatservice.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.chatservice.entity.enums.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Same ObjectMapper setup as {@link iuh.fit.chatservice.config.ChatJacksonConfig} / Valkey cache.
 */
class ChatMessageJsonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void roundTrip_messageWithAttachmentsAndReactions() throws Exception {
        MessageAttachmentDto attachment = MessageAttachmentDto.builder()
                .attachmentId("att-1")
                .fileType("IMAGE")
                .mimeType("image/jpeg")
                .url("https://cdn.example/photo.jpg")
                .s3Key("chat/message/u1/photo.jpg")
                .fileName("photo.jpg")
                .size(1024L)
                .build();

        MessageReactionDto reaction = MessageReactionDto.builder()
                .userId("user-2")
                .reactionType("LIKE")
                .createdAt(Instant.parse("2025-06-01T12:01:00Z"))
                .updatedAt(Instant.parse("2025-06-01T12:01:00Z"))
                .build();

        ChatMessage original = ChatMessage.builder()
                .messageId("msg-1")
                .conversationId("conv-1")
                .senderId("user-1")
                .type(MessageType.TEXT)
                .content("hello")
                .createdAt(Instant.parse("2025-06-01T12:00:00Z"))
                .attachmentCount(1)
                .reactionCount(1)
                .attachments(List.of(attachment))
                .reactions(List.of(reaction))
                .build();

        String json = objectMapper.writeValueAsString(original);
        ChatMessage restored = objectMapper.readValue(json, ChatMessage.class);

        assertEquals("msg-1", restored.getMessageId());
        assertEquals(MessageType.TEXT, restored.getType());
        assertEquals(Instant.parse("2025-06-01T12:00:00Z"), restored.getCreatedAt());
        assertNotNull(restored.getAttachments());
        assertEquals(1, restored.getAttachments().size());
        assertEquals("chat/message/u1/photo.jpg", restored.getAttachments().get(0).getS3Key());
        assertNotNull(restored.getReactions());
        assertEquals(1, restored.getReactions().size());
        assertEquals("LIKE", restored.getReactions().get(0).getReactionType());
    }
}
