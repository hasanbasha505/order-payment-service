package com.paymentservice.repositories;

import com.paymentservice.enums.TransactionStatus;
import com.paymentservice.enums.TransactionType;
import com.paymentservice.models.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);

    List<PaymentTransaction> findByPaymentIdAndTransactionType(UUID paymentId, TransactionType type);

    /**
     * Find transaction by payment ID and idempotency key.
     * Used for idempotency check at service level.
     */
    Optional<PaymentTransaction> findByPaymentIdAndIdempotencyKey(
        UUID paymentId,
        String idempotencyKey
    );

    /**
     * Find transaction by payment ID, type, and idempotency key.
     * Most specific idempotency check.
     */
    Optional<PaymentTransaction> findByPaymentIdAndTransactionTypeAndIdempotencyKey(
        UUID paymentId,
        TransactionType transactionType,
        String idempotencyKey
    );

    List<PaymentTransaction> findByPaymentIdAndStatus(UUID paymentId, TransactionStatus status);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = :status AND t.transactionType = :type")
    List<PaymentTransaction> findByStatusAndType(
        @Param("status") TransactionStatus status,
        @Param("type") TransactionType type
    );

    /**
     * Find transactions for reconciliation.
     */
    @Query("""
        SELECT t FROM PaymentTransaction t
        WHERE t.paymentId IN :paymentIds
        AND t.status = 'SUCCESS'
        ORDER BY t.paymentId, t.createdAt
        """)
    List<PaymentTransaction> findSuccessfulTransactions(@Param("paymentIds") List<UUID> paymentIds);

    /**
     * Count successful captures in time range.
     */
    @Query("""
        SELECT COUNT(t) FROM PaymentTransaction t
        WHERE t.transactionType = 'CAPTURE'
        AND t.status = 'SUCCESS'
        AND t.createdAt >= :startTime
        AND t.createdAt < :endTime
        """)
    long countSuccessfulCaptures(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
}
