package iuh.fit.chatservice.entity;

import iuh.fit.chatservice.entity.enums.MemberRole;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMember {

    private String id;

    private String userId;

    private MemberRole role;

    private String nickname;

    private String lastReadMessageId;

    private Instant lastReadAt;

    @Builder.Default
    private Boolean archived = false;

    @Builder.Default
    private Boolean deleted = false;

    @CreatedDate
    private Instant joinedAt;

}