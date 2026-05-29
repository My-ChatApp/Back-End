package iuh.fit.chatservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// event/payload/FriendRequestSentEvent.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestSentEvent {
    private String requestId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private Instant sentAt;
}