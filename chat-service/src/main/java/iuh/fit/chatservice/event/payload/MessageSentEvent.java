package iuh.fit.chatservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentEvent {
    private String messageId;
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private List<String> receiverIds;  // nhiều người nhận
    private Instant sentAt;
}