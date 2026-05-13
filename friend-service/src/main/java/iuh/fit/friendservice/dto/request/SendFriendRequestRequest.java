package iuh.fit.friendservice.dto.request;

import lombok.Data;

@Data
public class SendFriendRequestRequest {
    private String senderId;
    private String receiverId;
}