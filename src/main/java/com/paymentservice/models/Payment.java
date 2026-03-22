package com.paymentservice.models;

import com.paymentservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Payment entity representing the payment state machine.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_provider_payment_id", columnList = "provider_payment_id"),
    @Index(name = "idx_payments_captured_at", columnList = "captured_at"),
    @Index(name = "idx_payments_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "authorized_amount")
    private Long authorizedAmount;

    @Column(name = "captured_amount", nullable = false)
    @Builder.Default
    private Long capturedAmount = 0L;

    @Column(name = "refunded_amount", nullable = false)
    @Builder.Default
    private Long refundedAmount = 0L;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_provider", length = 50)
    private String paymentProvider;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "provider_auth_code")
    private String providerAuthCode;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @OneToMany(mappedBy = "payment", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<PaymentTransaction> transactions = new ArrayList<>();

    /**
     * Calculate the net amount (captured - refunded).
     */
    public Long getNetAmount() {
        return capturedAmount - refundedAmount;
    }

    /**
     * Calculate the available amount for refund.
     */
    public Long getAvailableForRefund() {
        return capturedAmount - refundedAmount;
    }

    /**
     * Check if authorization has expired.
     */
    public boolean isAuthorizationExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if status transition is valid.
     */
    public boolean canTransitionTo(PaymentStatus newStatus) {
        return status.canTransitionTo(newStatus);
    }

    /**
     * Transition to new status if valid.
     */
    public void transitionTo(PaymentStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", status, newStatus));
        }
        this.status = newStatus;
    }

    /**
     * Check if payment can be captured.
     */
    public boolean canCapture() {
        return status.canCapture() && !isAuthorizationExpired();
    }

    /**
     * Check if payment can be refunded.
     */
    public boolean canRefund() {
        return status.canRefund() && getAvailableForRefund() > 0;
    }
}
