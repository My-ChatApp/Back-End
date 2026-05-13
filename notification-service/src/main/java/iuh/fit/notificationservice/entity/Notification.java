package iuh.fit.notificationservice.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    private String userId;        // ai nhận thông báo
    private String type;          // FRIEND_REQUEST, FRIEND_ACCEPTED, MESSAGE...
    private String title;
    private String body;
    private String referenceId;   // id liên quan (requestId, messageId...)
    private boolean isRead;

    @CreatedDate
    private Instant createdAt;

    // tự xóa sau 30 ngày
    @Indexed(expireAfterSeconds = 2592000)
    private Instant expireAt;
}