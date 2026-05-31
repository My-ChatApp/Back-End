package iuh.fit.chatservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionDto {
    private String userId;
    private String reactionType;
    private Instant createdAt;
    private Instant updatedAt;
}
