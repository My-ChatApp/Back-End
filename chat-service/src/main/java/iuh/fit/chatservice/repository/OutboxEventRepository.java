package iuh.fit.chatservice.repository;

import iuh.fit.chatservice.entity.OutboxEvent;
import iuh.fit.chatservice.entity.enums.OutboxEventStatus;
import iuh.fit.chatservice.entity.enums.OutboxEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    Optional<OutboxEvent> findByAggregateIdAndEventTypeAndStatus(
            String aggregateId, OutboxEventType eventType, OutboxEventStatus status);

    @Query(
            value = """
                    SELECT * FROM app.outbox_events
                    WHERE status = 'PENDING'
                      AND next_retry_at <= NOW()
                    ORDER BY created_at
                    LIMIT :batch
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEvent> findPendingForPublish(@Param("batch") int batch);
}
