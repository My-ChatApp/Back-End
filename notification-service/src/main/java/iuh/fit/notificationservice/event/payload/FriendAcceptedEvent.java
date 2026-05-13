package iuh.fit.notificationservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// event/payload/FriendAcceptedEvent.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendAcceptedEvent {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String receiverName;
    private Instant acceptedAt;
}