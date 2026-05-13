package iuh.fit.chatservice;

import iuh.fit.chatservice.entity.Message;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "message_reactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReaction {

    @Id
    private String id;

    private Message message;

    private String userId;

    private String reactionType;

    private LocalDateTime createdAt;

    @CreatedDate
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}