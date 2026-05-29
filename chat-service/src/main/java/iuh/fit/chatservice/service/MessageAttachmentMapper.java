package iuh.fit.chatservice.service;

import iuh.fit.chatservice.dto.request.MessageAttachmentRequest;
import iuh.fit.chatservice.dto.request.SendMessageRequest;
import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.model.MessageAttachmentDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class MessageAttachmentMapper {

    public List<MessageAttachmentDto> toDtoList(List<MessageAttachmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<MessageAttachmentDto> result = new ArrayList<>(requests.size());
        for (MessageAttachmentRequest req : requests) {
            if (req == null) {
                continue;
            }
            String mimeType = normalizeMime(req.getMimeType());
            String fileType = resolveFileType(req.getFileType(), mimeType);
            String attachmentId = req.getAttachmentId();
            if (attachmentId == null || attachmentId.isBlank()) {
                attachmentId = UUID.randomUUID().toString();
            }
            result.add(MessageAttachmentDto.builder()
                    .attachmentId(attachmentId)
                    .fileType(fileType)
                    .mimeType(mimeType)
                    .url(req.getUrl())
                    .s3Key(req.getS3Key())
                    .fileName(req.getFileName())
                    .size(req.getSize())
                    .width(req.getWidth())
                    .height(req.getHeight())
                    .duration(req.getDuration())
                    .thumbnailUrl(req.getThumbnailUrl())
                    .build());
        }
        return result;
    }

    public MessageType resolveType(SendMessageRequest req, List<MessageAttachmentDto> attachments) {
        if (attachments != null && !attachments.isEmpty()) {
            return MessageType.FILE;
        }
        String rawType = req != null ? req.getType() : null;
        if (rawType == null || rawType.isBlank() || "TEXT".equalsIgnoreCase(rawType)) {
            return MessageType.TEXT;
        }
        return MessageType.FILE;
    }

    public void validate(MessageType type, List<MessageAttachmentDto> attachments, String content) {
        if (type == MessageType.TEXT) {
            if (attachments != null && !attachments.isEmpty()) {
                throw new IllegalArgumentException("TEXT messages must not include attachments");
            }
            requireNonBlank(content, "message content");
            return;
        }
        if (attachments == null || attachments.isEmpty()) {
            throw new IllegalArgumentException("FILE messages require at least one attachment");
        }
        for (MessageAttachmentDto attachment : attachments) {
            requireNonBlank(attachment.getUrl(), "attachment url");
            requireNonBlank(attachment.getS3Key(), "attachment s3Key");
            requireNonBlank(attachment.getFileName(), "attachment fileName");
            requireNonBlank(attachment.getMimeType(), "attachment mimeType");
        }
    }

    public static String inferFileType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "OTHER";
        }
        String lower = mimeType.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith("image/")) {
            return "IMAGE";
        }
        if (lower.startsWith("video/")) {
            return "VIDEO";
        }
        if (lower.startsWith("audio/")) {
            return "AUDIO";
        }
        if (lower.equals("application/pdf")
                || lower.equals("application/msword")
                || lower.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || lower.equals("application/vnd.ms-excel")
                || lower.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || lower.equals("application/vnd.ms-powerpoint")
                || lower.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                || lower.startsWith("text/")) {
            return "DOCUMENT";
        }
        return "OTHER";
    }

    public static String buildLastMessagePreview(
            MessageType type, String content, List<MessageAttachmentDto> attachments) {
        if (type == MessageType.TEXT) {
            if (content == null) {
                return "";
            }
            return truncate(content, 500);
        }
        return buildFilePreview(attachments, content);
    }

    public static String buildFilePreview(List<MessageAttachmentDto> attachments, String content) {
        if (content != null && !content.isBlank()) {
            return truncate(content, 500);
        }
        if (attachments == null || attachments.isEmpty()) {
            return "[File]";
        }
        MessageAttachmentDto first = attachments.get(0);
        String fileName = first.getFileName() != null ? first.getFileName() : "file";
        String label = previewLabelForFileType(first.getFileType());
        return label + " " + fileName;
    }

    private static String previewLabelForFileType(String fileType) {
        if (fileType == null) {
            return "[File]";
        }
        return switch (fileType.toUpperCase(Locale.ROOT)) {
            case "IMAGE" -> "[Ảnh]";
            case "VIDEO" -> "[Video]";
            case "AUDIO" -> "[Audio]";
            case "DOCUMENT" -> "[Tài liệu]";
            default -> "[File]";
        };
    }

    private static String resolveFileType(String requested, String mimeType) {
        if (requested != null && !requested.isBlank()) {
            return requested.toUpperCase(Locale.ROOT);
        }
        return inferFileType(mimeType);
    }

    private static String normalizeMime(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        String trimmed = mimeType.trim();
        int semi = trimmed.indexOf(';');
        if (semi >= 0) {
            trimmed = trimmed.substring(0, semi).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FILE message attachment requires " + fieldName);
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen);
    }
}
