package iuh.fit.chatservice.event.payload;

import iuh.fit.chatservice.entity.Conversation;
import iuh.fit.chatservice.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatInboxEvent {

    public enum EventType {
        CONVERSATION_CREATED,
        CONVERSATION_UPDATED,
        CONVERSATION_DELETED,
        MESSAGE_CREATED
    }

    private EventType eventType;
    private Conversation conversation;
    private ChatMessage message;
}
