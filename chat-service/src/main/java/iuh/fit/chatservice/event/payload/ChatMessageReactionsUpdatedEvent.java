package iuh.fit.chatservice.event.payload;

import iuh.fit.chatservice.model.MessageReactionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageReactionsUpdatedEvent {

    private String messageId;
    private String conversationId;
    private int reactionCount;

    @Builder.Default
    private List<MessageReactionDto> reactions = new ArrayList<>();
}
