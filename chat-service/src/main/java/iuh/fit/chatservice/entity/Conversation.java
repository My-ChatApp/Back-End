package iuh.fit.chatservice.entity;

import iuh.fit.chatservice.entity.enums.TypeRoom;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    private String id;
    private TypeRoom type;
    private String title;
    private String avatarUrl;
    private String createdBy;
    private String lastMessageId;
    private Instant lastMessageAt;

    @Builder.Default
    private Boolean deleted = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Builder.Default
    private List<ConversationMember> members = new ArrayList<>();
}