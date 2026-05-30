package iuh.fit.chatservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistoryLoadRequestedEvent {

    private String conversationId;
    private String userId;
    private String beforeMessageId;
    private Instant beforeCreatedAt;
    private int limit;
}
