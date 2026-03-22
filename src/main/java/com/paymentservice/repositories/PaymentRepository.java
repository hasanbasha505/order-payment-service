package com.paymentservice.repositories;

import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.models.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find payment by ID with pessimistic write lock.
     * CRITICAL: Used for capture and refund operations to prevent race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithLock(@Param("paymentId") UUID paymentId);

    Optional<Payment> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") UUID orderId);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.capturedAt >= :startTime AND p.capturedAt < :endTime")
    Page<Payment> findByStatusAndCapturedAtBetween(
        @Param("status") PaymentStatus status,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable
    );

    @Query("SELECT p FROM Payment p JOIN FETCH p.transactions WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithTransactions(@Param("paymentId") UUID paymentId);

    @Query("SELECT p FROM Payment p WHERE p.providerPaymentId = :providerPaymentId")
    Optional<Payment> findByProviderPaymentId(@Param("providerPaymentId") String providerPaymentId);

    /**
     * Find payments for reconciliation - captured within a time range.
     */
    @Query("""
        SELECT p FROM Payment p
        JOIN p.order o
        WHERE o.restaurantId = :restaurantId
        AND p.status IN ('CAPTURED', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED')
        AND p.capturedAt >= :startTime
        AND p.capturedAt < :endTime
        """)
    List<Payment> findForReconciliation(
        @Param("restaurantId") UUID restaurantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
}
