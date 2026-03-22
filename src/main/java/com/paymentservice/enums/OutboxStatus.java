package com.paymentservice.enums;

/**
 * Status of an outbox event.
 */
public enum OutboxStatus {
    PENDING,    // Event waiting to be published
    PUBLISHED,  // Event successfully published to Kafka
    FAILED      // Event publishing failed after retries
}
