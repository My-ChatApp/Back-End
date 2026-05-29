package iuh.fit.notificationservice.dto.request;

import iuh.fit.notificationservice.entity.NotificationType;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateNotificationRequest {
    private UUID userId;
    private UUID actorId;
    private NotificationType type;
    private String title;
    private String body;
    private UUID referenceId;
    private Map<String, Object> data;
    private Instant expireAt;
}
