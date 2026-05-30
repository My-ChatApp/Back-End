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
public class UserPresenceNotifyEvent {
    private String userId;
    private String targetUserId;
    private String displayName;
    private boolean online;
    private Instant occurredAt;
}
