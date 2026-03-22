package com.paymentservice.models;

import com.paymentservice.enums.MatchStatus;
import com.paymentservice.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Daily reconciliation report per restaurant.
 */
@Entity
@Table(name = "reconciliation_reports", indexes = {
    @Index(name = "idx_reconciliation_restaurant_id", columnList = "restaurant_id"),
    @Index(name = "idx_reconciliation_report_date", columnList = "report_date"),
    @Index(name = "idx_reconciliation_status", columnList = "status"),
    @Index(name = "idx_reconciliation_match_status", columnList = "match_status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_reconciliation_restaurant_date",
                      columnNames = {"restaurant_id", "report_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    // Internal totals (from our database)
    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_captured_amount", nullable = false)
    @Builder.Default
    private Long totalCapturedAmount = 0L;

    @Column(name = "total_refunded_amount", nullable = false)
    @Builder.Default
    private Long totalRefundedAmount = 0L;

    @Column(name = "net_amount", nullable = false)
    @Builder.Default
    private Long netAmount = 0L;

    // Provider totals (from payment provider)
    @Column(name = "provider_total_captured")
    private Long providerTotalCaptured;

    @Column(name = "provider_total_refunded")
    private Long providerTotalRefunded;

    @Column(name = "provider_net_amount")
    private Long providerNetAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", length = 20)
    @Builder.Default
    private MatchStatus matchStatus = MatchStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_data", columnDefinition = "jsonb")
    private Map<String, Object> reportData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discrepancies", columnDefinition = "jsonb")
    private Map<String, Object> discrepancies;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "investigated_at")
    private Instant investigatedAt;

    @Column(name = "investigated_by", length = 100)
    private String investigatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", insertable = false, updatable = false)
    private Restaurant restaurant;

    /**
     * Check if internal and provider totals match.
     */
    public boolean isMatched() {
        if (providerNetAmount == null) {
            return false;
        }
        return netAmount.equals(providerNetAmount);
    }

    /**
     * Calculate the variance between internal and provider net amounts.
     */
    public Long getVariance() {
        if (providerNetAmount == null) {
            return null;
        }
        return netAmount - providerNetAmount;
    }
}
