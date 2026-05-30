package iuh.fit.chatservice.dto.request;

import lombok.Data;

@Data
public class UpdateConversationRequest {
    private String title;
    private String description;
    private String avatarUrl;
}
