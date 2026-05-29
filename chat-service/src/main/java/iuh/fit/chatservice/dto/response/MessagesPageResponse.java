package iuh.fit.chatservice.dto.response;

import iuh.fit.chatservice.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessagesPageResponse {

    private List<ChatMessage> messages;
    private boolean loading;
    private boolean hasMore;
}
