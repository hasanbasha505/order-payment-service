package com.paymentservice.repositories;

import com.paymentservice.enums.OutboxStatus;
import com.paymentservice.models.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find pending events for publishing with lock.
     * Uses SKIP LOCKED to allow concurrent publishers.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE status = 'PENDING'
        ORDER BY created_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findPendingEventsForUpdate(@Param("limit") int limit);

    List<OutboxEvent> findByStatus(OutboxStatus status);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, UUID aggregateId);

    /**
     * Delete old published events for cleanup.
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :cutoff")
    int deletePublishedEventsBefore(@Param("cutoff") Instant cutoff);

    /**
     * Count pending events (for monitoring).
     */
    long countByStatus(OutboxStatus status);

    /**
     * Find pending events for publishing (non-locking version for simple use).
     */
    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at LIMIT :limit",
           nativeQuery = true)
    List<OutboxEvent> findPendingEventsForPublishing(@Param("limit") int limit);
}
