package iuh.fit.chatservice.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.model.MessageAttachmentDto;
import iuh.fit.chatservice.service.MessageAttachmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Mirrors STOMP JSON from Front-End buildFileSendPayload (attachments[] + type FILE).
 */
class SendMessageRequestJsonTest {

    private ObjectMapper objectMapper;
    private final MessageAttachmentMapper mapper = new MessageAttachmentMapper();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void deserialize_fileMessageWithAttachments() throws Exception {
        String json = """
                {
                  "conversationId": "conv-1",
                  "senderId": "user-1",
                  "content": "",
                  "type": "FILE",
                  "attachments": [{
                    "attachmentId": "att-1",
                    "fileType": "IMAGE",
                    "mimeType": "image/jpeg",
                    "url": "https://cdn.example/photo.jpg",
                    "s3Key": "chat/message/user-1/photo.jpg",
                    "fileName": "photo.jpg",
                    "size": 1024
                  }]
                }
                """;

        SendMessageRequest req = objectMapper.readValue(json, SendMessageRequest.class);

        assertEquals("conv-1", req.getConversationId());
        assertEquals("FILE", req.getType());
        assertNotNull(req.getAttachments());
        assertEquals(1, req.getAttachments().size());
        assertEquals("chat/message/user-1/photo.jpg", req.getAttachments().get(0).getS3Key());

        var dtos = mapper.toDtoList(req.getAttachments());
        MessageType type = mapper.resolveType(req, dtos);
        mapper.validate(type, dtos, req.getContent());

        assertEquals(MessageType.FILE, type);
        assertEquals(1, dtos.size());
        assertEquals("IMAGE", dtos.get(0).getFileType());
    }
}
