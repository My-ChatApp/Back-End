package iuh.fit.friendservice.repository;

import iuh.fit.friendservice.entity.FriendRequest;
import iuh.fit.friendservice.entity.enums.FriendRequestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {
    boolean existsBySenderIdAndReceiverIdAndStatus(
        String senderId,
        String receiverId,
        FriendRequestStatus status
    );
}