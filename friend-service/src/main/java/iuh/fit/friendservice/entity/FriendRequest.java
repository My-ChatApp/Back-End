package iuh.fit.friendservice.entity;

import iuh.fit.friendservice.entity.enums.FriendRequestStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "friend_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequest {

    @Id
    private String id;

    private String senderId;    // A gửi lời mời
    private String receiverId;  // B nhận lời mời

    private FriendRequestStatus status; // PENDING, ACCEPTED, REJECTED

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}