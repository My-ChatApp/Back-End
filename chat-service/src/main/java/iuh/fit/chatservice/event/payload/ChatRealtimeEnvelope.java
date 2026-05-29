package iuh.fit.chatservice.event.payload;

import iuh.fit.chatservice.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRealtimeEnvelope {

    public enum EventType {
        MESSAGE_CREATED,
        MESSAGE_UPDATED,
        MESSAGE_DELETED
    }

    private EventType eventType;
    private ChatMessage message;
}
