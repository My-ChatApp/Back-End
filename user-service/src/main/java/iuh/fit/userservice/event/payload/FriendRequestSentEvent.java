package iuh.fit.userservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestSentEvent {
    private String requestId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private Instant sentAt;
}
