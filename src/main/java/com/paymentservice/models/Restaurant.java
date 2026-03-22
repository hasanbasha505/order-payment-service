package com.paymentservice.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Restaurant entity with timezone information for reconciliation.
 */
@Entity
@Table(name = "restaurants", indexes = {
    @Index(name = "idx_restaurants_timezone", columnList = "timezone"),
    @Index(name = "idx_restaurants_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    /**
     * Get the ZoneId for this restaurant's timezone.
     */
    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }
}
