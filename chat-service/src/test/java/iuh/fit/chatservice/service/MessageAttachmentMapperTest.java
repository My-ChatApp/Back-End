package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.MessageAttachmentRequest;
import iuh.fit.chatservice.dto.request.SendMessageRequest;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.model.MessageAttachmentDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageAttachmentMapperTest {

    private final MessageAttachmentMapper mapper = new MessageAttachmentMapper();

    @Test
    void inferFileType_image() {
        assertEquals("IMAGE", MessageAttachmentMapper.inferFileType("image/jpeg"));
    }

    @Test
    void inferFileType_video() {
        assertEquals("VIDEO", MessageAttachmentMapper.inferFileType("video/mp4"));
    }

    @Test
    void inferFileType_document() {
        assertEquals("DOCUMENT", MessageAttachmentMapper.inferFileType("application/pdf"));
    }

    @Test
    void resolveType_withAttachments_isFile() {
        SendMessageRequest req = new SendMessageRequest();
        req.setType("TEXT");
        List<MessageAttachmentDto> attachments = List.of(sampleAttachment());
        assertEquals(MessageType.FILE, mapper.resolveType(req, attachments));
    }

    @Test
    void resolveType_withoutAttachments_isText() {
        SendMessageRequest req = new SendMessageRequest();
        req.setType("TEXT");
        assertEquals(MessageType.TEXT, mapper.resolveType(req, List.of()));
    }

    @Test
    void buildFilePreview_usesFileTypeLabel() {
        MessageAttachmentDto attachment = MessageAttachmentDto.builder()
                .fileType("IMAGE")
                .fileName("photo.jpg")
                .build();
        assertEquals("[Ảnh] photo.jpg", MessageAttachmentMapper.buildFilePreview(List.of(attachment), null));
    }

    @Test
    void buildLastMessagePreview_fileWithoutCaption() {
        MessageAttachmentDto attachment = MessageAttachmentDto.builder()
                .fileType("IMAGE")
                .fileName("photo.jpg")
                .build();
        assertEquals(
                "[Ảnh] photo.jpg",
                MessageAttachmentMapper.buildLastMessagePreview(MessageType.FILE, null, List.of(attachment)));
    }

    @Test
    void buildFilePreview_prefersCaption() {
        MessageAttachmentDto attachment = MessageAttachmentDto.builder()
                .fileType("IMAGE")
                .fileName("photo.jpg")
                .build();
        assertEquals("Hello", MessageAttachmentMapper.buildFilePreview(List.of(attachment), "Hello"));
    }

    @Test
    void validate_textRequiresNonBlankContent() {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.validate(MessageType.TEXT, List.of(), "  "));
    }

    @Test
    void validate_fileRequiresAttachmentFields() {
        MessageAttachmentDto incomplete = MessageAttachmentDto.builder()
                .attachmentId("a1")
                .mimeType("image/jpeg")
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> mapper.validate(MessageType.FILE, List.of(incomplete), null));
    }

    private static MessageAttachmentDto sampleAttachment() {
        return MessageAttachmentDto.builder()
                .attachmentId("a1")
                .fileType("IMAGE")
                .mimeType("image/jpeg")
                .url("https://cdn.example/photo.jpg")
                .s3Key("chat/inbox/u1/photo.jpg")
                .fileName("photo.jpg")
                .build();
    }
}
