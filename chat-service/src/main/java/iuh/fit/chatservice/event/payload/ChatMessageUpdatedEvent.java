package iuh.fit.chatservice.event.payload;

import iuh.fit.chatservice.entity.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageUpdatedEvent {

    private String messageId;
    private String conversationId;
    private String senderId;
    private MessageType type;
    private String content;
    private boolean edited;
    private Instant editedAt;
    private Instant createdAt;
}
