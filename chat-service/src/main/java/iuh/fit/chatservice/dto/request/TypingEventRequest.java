package iuh.fit.chatservice.dto.request;

import lombok.Data;

@Data
public class TypingEventRequest {
    private String conversationId;
    private String userId;
    private boolean typing;
}
