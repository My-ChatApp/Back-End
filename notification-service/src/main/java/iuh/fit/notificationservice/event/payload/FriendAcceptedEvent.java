package iuh.fit.notificationservice.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FriendAcceptedEvent {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String receiverName;
    private Instant acceptedAt;
}
