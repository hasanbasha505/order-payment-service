package com.paymentservice.models;

import com.paymentservice.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Order entity representing a customer's purchase.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_restaurant_id", columnList = "restaurant_id"),
    @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_created_at", columnList = "created_at"),
    @Index(name = "idx_orders_restaurant_created", columnList = "restaurant_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", insertable = false, updatable = false)
    private Restaurant restaurant;

    @OneToOne(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Payment payment;

    /**
     * Check if status transition is valid.
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return status.canTransitionTo(newStatus);
    }

    /**
     * Transition to new status if valid.
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", status, newStatus));
        }
        this.status = newStatus;
    }
}
