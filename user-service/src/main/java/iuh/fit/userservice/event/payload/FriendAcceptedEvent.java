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
public class FriendAcceptedEvent {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String receiverName;
    private Instant acceptedAt;
}
