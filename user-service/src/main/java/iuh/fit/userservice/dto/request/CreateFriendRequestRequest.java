package iuh.fit.userservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFriendRequestRequest {
    private String senderId;
    private String receiverId;
    private String message;
}
