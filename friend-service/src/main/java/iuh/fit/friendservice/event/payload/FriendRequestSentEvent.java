package iuh.fit.friendservice.event.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// Khi A gửi lời mời
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