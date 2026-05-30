package iuh.fit.chatservice.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SendMessageRequest {
    private String conversationId;
    private String senderId;
    private String content;
    private String type; // TEXT, FILE, legacy IMAGE/VIDEO
    private List<MessageAttachmentRequest> attachments = new ArrayList<>();
}