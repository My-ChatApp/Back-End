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
public class UserPresenceChangedEvent {
    private String userId;
    private boolean online;
    private Instant occurredAt;
}
