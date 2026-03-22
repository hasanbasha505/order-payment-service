package com.paymentservice.repositories;

import com.paymentservice.models.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByKey(String key);

    boolean existsByKey(String key);

    /**
     * Delete expired idempotency keys.
     * Called by cleanup scheduled job.
     */
    @Modifying
    @Query("DELETE FROM IdempotencyKey i WHERE i.expiresAt < :now")
    int deleteExpiredKeys(@Param("now") Instant now);
}
