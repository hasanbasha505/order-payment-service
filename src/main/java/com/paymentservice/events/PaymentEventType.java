package com.paymentservice.events;

/**
 * Types of payment events published to Kafka.
 */
public enum PaymentEventType {
    PAYMENT_CREATED,
    PAYMENT_AUTHORIZED,
    PAYMENT_AUTHORIZATION_FAILED,
    PAYMENT_CAPTURED,
    PAYMENT_CAPTURE_FAILED,
    PAYMENT_REFUNDED,
    PAYMENT_REFUND_FAILED,
    PAYMENT_CANCELLED
}
