package com.paymentservice.models;

import com.paymentservice.enums.TransactionStatus;
import com.paymentservice.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable payment transaction record for audit trail.
 * This table is append-only - records are never updated or deleted.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_txn_payment_id", columnList = "payment_id"),
    @Index(name = "idx_payment_txn_created_at", columnList = "created_at"),
    @Index(name = "idx_payment_txn_type_status", columnList = "transaction_type, status"),
    @Index(name = "idx_payment_txn_provider_txn_id", columnList = "provider_txn_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.INITIATED;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "provider_txn_id")
    private String providerTxnId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_response", columnDefinition = "jsonb")
    private Map<String, Object> providerResponse;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", insertable = false, updatable = false)
    private Payment payment;

    /**
     * Check if transaction was successful.
     */
    public boolean isSuccess() {
        return status == TransactionStatus.SUCCESS;
    }

    /**
     * Check if transaction failed.
     */
    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }

    /**
     * Check if transaction timed out.
     */
    public boolean isTimeout() {
        return status == TransactionStatus.TIMEOUT;
    }
}
