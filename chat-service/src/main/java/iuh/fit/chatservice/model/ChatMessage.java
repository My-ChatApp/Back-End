package iuh.fit.chatservice.model;

import iuh.fit.chatservice.entity.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String messageId;
    private String conversationId;
    private String senderId;
    private MessageType type;
    private String content;
    private String replyToMessageId;
    private String replyToPreview;

    @Builder.Default
    private boolean edited = false;

    @Builder.Default
    private boolean deleted = false;

    private Instant editedAt;
    private Instant deletedAt;
    private Instant createdAt;

    @Builder.Default
    private int attachmentCount = 0;

    @Builder.Default
    private int reactionCount = 0;

    @Builder.Default
    private List<MessageAttachmentDto> attachments = new ArrayList<>();

    @Builder.Default
    private List<MessageReactionDto> reactions = new ArrayList<>();
}
