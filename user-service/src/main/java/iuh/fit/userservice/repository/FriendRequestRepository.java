package iuh.fit.userservice.repository;

import iuh.fit.userservice.entity.friend.FriendRequest;
import iuh.fit.userservice.entity.friend.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    boolean existsBySenderIdAndReceiverIdAndStatus(
            UUID senderId, UUID receiverId, FriendRequestStatus status);

    List<FriendRequest> findByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status);

    List<FriendRequest> findBySenderIdAndStatus(UUID senderId, FriendRequestStatus status);

    @Query("""
            SELECT fr FROM FriendRequest fr
            WHERE fr.status = :status
              AND (:userId = fr.senderId OR :userId = fr.receiverId)
            ORDER BY fr.updatedAt DESC
            """)
    List<FriendRequest> findFriendsByUserId(
            @Param("userId") UUID userId,
            @Param("status") FriendRequestStatus status);

    @Query("""
            SELECT fr FROM FriendRequest fr
            WHERE fr.status = :status
              AND ((fr.senderId = :userId AND fr.receiverId = :friendUserId)
                OR (fr.senderId = :friendUserId AND fr.receiverId = :userId))
            """)
    Optional<FriendRequest> findAcceptedBetween(
            @Param("userId") UUID userId,
            @Param("friendUserId") UUID friendUserId,
            @Param("status") FriendRequestStatus status);
}
