package iuh.fit.chatservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageReactionDto {
    private String userId;
    private String reactionType;
    private Instant createdAt;
    private Instant updatedAt;
}
