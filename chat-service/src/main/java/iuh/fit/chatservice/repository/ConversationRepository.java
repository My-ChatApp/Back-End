package iuh.fit.chatservice.repository;

import iuh.fit.chatservice.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findAllByDeletedFalse();

    Optional<Conversation> findByIdAndDeletedFalse(UUID id);

    @Query("""
            SELECT DISTINCT c FROM Conversation c
            LEFT JOIN FETCH c.members
            WHERE c.deleted = false
              AND c.id IN (
                  SELECT c2.id FROM Conversation c2
                  JOIN c2.members m
                  WHERE m.id.userId = :userId AND m.deleted = false AND c2.deleted = false
              )
            ORDER BY c.lastMessageAt DESC, c.updatedAt DESC
            """)
    List<Conversation> findByMemberUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT c FROM Conversation c
            LEFT JOIN FETCH c.members
            WHERE c.id = :id AND c.deleted = false
            """)
    Optional<Conversation> findByIdWithMembers(@Param("id") UUID id);

    @Query(value = """
            SELECT c.id FROM app.conversations c
            INNER JOIN app.conversation_members m1
                ON m1.conversation_id = c.id AND m1.user_id = :u1 AND m1.deleted = false
            INNER JOIN app.conversation_members m2
                ON m2.conversation_id = c.id AND m2.user_id = :u2 AND m2.deleted = false
            WHERE c.type = 'PRIVATE' AND c.deleted = false
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findPrivateConversationIdBetweenUsers(@Param("u1") UUID u1, @Param("u2") UUID u2);

    default Optional<Conversation> findPrivateBetweenUsers(UUID u1, UUID u2) {
        return findPrivateConversationIdBetweenUsers(u1, u2).flatMap(this::findByIdWithMembers);
    }
}
