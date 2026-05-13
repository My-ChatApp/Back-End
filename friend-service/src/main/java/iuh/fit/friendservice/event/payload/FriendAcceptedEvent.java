package iuh.fit.friendservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// Khi B chấp nhận
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendAcceptedEvent {
    private String requestId;
    private String senderId;    // A
    private String receiverId;  // B
    private String receiverName;
    private Instant acceptedAt;
}