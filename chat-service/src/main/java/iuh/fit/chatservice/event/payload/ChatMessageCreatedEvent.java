package iuh.fit.chatservice.event.payload;

import iuh.fit.chatservice.entity.enums.MessageType;
import iuh.fit.chatservice.model.MessageAttachmentDto;
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
public class ChatMessageCreatedEvent {

    private String messageId;
    private String conversationId;
    private String senderId;
    private MessageType type;
    private String content;
    private String replyToMessageId;
    private String replyToPreview;
    private boolean edited;
    private boolean deleted;
    private Instant createdAt;
    private int attachmentCount;
    private int reactionCount;

    @Builder.Default
    private List<MessageAttachmentDto> attachments = new ArrayList<>();

    private List<String> receiverIds;
}
