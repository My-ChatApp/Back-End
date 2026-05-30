package iuh.fit.chatservice.dto.response;

import iuh.fit.chatservice.entity.enums.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageSearchResult {
    private String messageId;
    private String conversationId;
    private String senderId;
    private MessageType type;
    private String content;
    private Instant createdAt;
}
