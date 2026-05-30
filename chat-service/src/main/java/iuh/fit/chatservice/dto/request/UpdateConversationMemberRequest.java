package iuh.fit.chatservice.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateConversationMemberRequest {
    private String nickname;
    private Boolean muted;
    private Boolean pinned;
    private Boolean notificationsEnabled;
    private Boolean archived;
    private UUID lastReadMessageId;
    private Integer unreadCount;
}
