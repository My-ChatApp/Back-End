package iuh.fit.chatservice.event.payload;

import iuh.fit.chatservice.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRealtimeEnvelope {

    public enum EventType {
        MESSAGE_CREATED,
        MESSAGE_UPDATED,
        MESSAGE_DELETED,
        TYPING,
        READ_RECEIPT
    }

    private EventType eventType;
    private ChatMessage message;

    /** TYPING / READ_RECEIPT */
    private String conversationId;
    private String userId;

    /** TYPING */
    private Boolean typing;

    /** READ_RECEIPT */
    private String lastReadMessageId;
    private Instant lastReadAt;
}
