package iuh.fit.chatservice.dto.request;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String conversationId;
    private String senderId;
    private String content;
    private String type; // TEXT, IMAGE...
}