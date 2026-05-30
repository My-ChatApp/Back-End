package iuh.fit.chatservice.repository;

import iuh.fit.chatservice.entity.ConversationMember;
import iuh.fit.chatservice.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    List<ConversationMember> findById_ConversationIdAndDeletedFalse(UUID conversationId);

    boolean existsById_ConversationIdAndId_UserIdAndDeletedFalse(UUID conversationId, UUID userId);

    @Modifying
    @Query("""
            UPDATE ConversationMember m
            SET m.unreadCount = m.unreadCount + 1, m.updatedAt = CURRENT_TIMESTAMP
            WHERE m.id.conversationId = :conversationId
              AND m.id.userId <> :senderId
              AND m.deleted = false
            """)
    int incrementUnreadForOthers(@Param("conversationId") UUID conversationId,
                                 @Param("senderId") UUID senderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ConversationMember m
            SET m.unreadCount = 0,
                m.lastReadAt = CURRENT_TIMESTAMP,
                m.updatedAt = CURRENT_TIMESTAMP
            WHERE m.id.conversationId = :conversationId
              AND m.id.userId = :userId
              AND m.deleted = false
            """)
    int resetUnread(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);
}
