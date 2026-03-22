package com.paymentservice.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payment event for Kafka publishing.
 * These events are published via the transactional outbox pattern.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentEvent {

    /**
     * Unique event ID.
     */
    private UUID eventId;

    /**
     * Type of event.
     */
    private PaymentEventType eventType;

    /**
     * Payment ID this event relates to.
     */
    private UUID paymentId;

    /**
     * Order ID associated with the payment.
     */
    private UUID orderId;

    /**
     * Restaurant ID.
     */
    private UUID restaurantId;

    /**
     * Amount involved in cents.
     */
    private Long amount;

    /**
     * Currency code.
     */
    private String currencyCode;

    /**
     * Payment status after this event.
     */
    private String paymentStatus;

    /**
     * Transaction ID if applicable.
     */
    private UUID transactionId;

    /**
     * Provider transaction reference.
     */
    private String providerTransactionId;

    /**
     * Timestamp when the event occurred.
     */
    private Instant occurredAt;

    /**
     * Idempotency key that triggered this event.
     */
    private String idempotencyKey;

    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;

    /**
     * Create a payment event with standard fields.
     */
    public static PaymentEvent create(
            PaymentEventType eventType,
            UUID paymentId,
            UUID orderId,
            UUID restaurantId,
            Long amount,
            String currencyCode,
            String paymentStatus) {

        return PaymentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .paymentId(paymentId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .amount(amount)
                .currencyCode(currencyCode)
                .paymentStatus(paymentStatus)
                .occurredAt(Instant.now())
                .build();
    }
}
