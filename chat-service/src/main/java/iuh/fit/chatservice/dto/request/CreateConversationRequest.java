package iuh.fit.chatservice.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateConversationRequest {
    private String title;
    private String type;                // "PRIVATE" hoặc "GROUP"
    private List<String> memberIds;     // ["userA", "userB"]
}