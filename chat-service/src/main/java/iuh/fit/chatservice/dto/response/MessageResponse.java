package iuh.fit.chatservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private String type;
    private LocalDateTime createdAt;
}