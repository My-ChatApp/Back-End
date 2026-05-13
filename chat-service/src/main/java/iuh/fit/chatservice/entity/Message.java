package iuh.fit.chatservice.entity;

import iuh.fit.chatservice.MessageReaction;
import iuh.fit.chatservice.entity.enums.MessageType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    private String id;

    private Conversation conversation;

    private String senderId;

    private MessageType type;

    private String content;

    private String replyToMessageId;

    @Builder.Default
    private Boolean edited = false;

    @Builder.Default
    private Boolean deleted = false;

    private Instant editedAt;

    private Instant deletedAt;
    @CreatedDate
    private Instant createdAt;

    @Builder.Default
    private List<MessageAttachment> attachments = new ArrayList<>();

    @Builder.Default
    private List<MessageReaction> reactions = new ArrayList<>();

}