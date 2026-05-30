package iuh.fit.chatservice.event.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.model.MessageAttachmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Same ObjectMapper setup as {@link iuh.fit.chatservice.config.ChatJacksonConfig} / RabbitMQ.
 */
class ChatMessageCreatedEventJsonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void roundTrip_fileEventWithAttachments() throws Exception {
        MessageAttachmentDto attachment = MessageAttachmentDto.builder()
                .attachmentId("att-1")
                .fileType("IMAGE")
                .mimeType("image/jpeg")
                .url("https://cdn.example/photo.jpg")
                .s3Key("chat/message/u1/photo.jpg")
                .fileName("photo.jpg")
                .size(1024L)
                .build();

        ChatMessageCreatedEvent original = ChatMessageCreatedEvent.builder()
                .messageId("msg-file-1")
                .conversationId("conv-1")
                .senderId("user-1")
                .type(MessageType.FILE)
                .content("")
                .createdAt(Instant.parse("2025-06-01T12:00:00Z"))
                .attachmentCount(1)
                .attachments(List.of(attachment))
                .receiverIds(List.of("user-2"))
                .build();

        String json = objectMapper.writeValueAsString(original);
        ChatMessageCreatedEvent restored = objectMapper.readValue(json, ChatMessageCreatedEvent.class);

        assertEquals(MessageType.FILE, restored.getType());
        assertEquals(1, restored.getAttachmentCount());
        assertNotNull(restored.getAttachments());
        assertEquals(1, restored.getAttachments().size());
        assertEquals("chat/message/u1/photo.jpg", restored.getAttachments().get(0).getS3Key());
        assertEquals(Instant.parse("2025-06-01T12:00:00Z"), restored.getCreatedAt());
    }
}
