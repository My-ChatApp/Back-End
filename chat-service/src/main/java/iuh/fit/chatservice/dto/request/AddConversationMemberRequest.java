package iuh.fit.chatservice.dto.request;

import iuh.fit.chatservice.entity.enums.MemberRole;
import lombok.Data;

import java.util.UUID;

@Data
public class AddConversationMemberRequest {
    private UUID userId;
    private MemberRole role;
    private String nickname;
    private UUID invitedBy;
}
