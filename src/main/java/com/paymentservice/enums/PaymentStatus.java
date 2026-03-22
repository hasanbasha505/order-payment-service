package com.paymentservice.enums;

import java.util.Map;
import java.util.Set;

/**
 * Payment status representing the payment state machine.
 *
 * State transitions:
 * PENDING -> AUTHORIZED | AUTH_FAILED | CANCELLED
 * AUTHORIZED -> CAPTURE_PENDING | CANCELLED
 * CAPTURE_PENDING -> CAPTURED | CAPTURE_FAILED | AUTHORIZED (revert on timeout)
 * CAPTURED -> REFUND_PENDING | PARTIALLY_REFUNDED | FULLY_REFUNDED
 * REFUND_PENDING -> PARTIALLY_REFUNDED | FULLY_REFUNDED | CAPTURED (revert on failure)
 * PARTIALLY_REFUNDED -> REFUND_PENDING | FULLY_REFUNDED
 */
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    AUTH_FAILED,
    CAPTURE_PENDING,
    CAPTURED,
    CAPTURE_FAILED,
    REFUND_PENDING,
    PARTIALLY_REFUNDED,
    FULLY_REFUNDED,
    CANCELLED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = Map.ofEntries(
        Map.entry(PENDING, Set.of(AUTHORIZED, AUTH_FAILED, CANCELLED)),
        Map.entry(AUTHORIZED, Set.of(CAPTURE_PENDING, CANCELLED)),
        Map.entry(CAPTURE_PENDING, Set.of(CAPTURED, CAPTURE_FAILED, AUTHORIZED)),
        Map.entry(CAPTURED, Set.of(REFUND_PENDING, PARTIALLY_REFUNDED, FULLY_REFUNDED)),
        Map.entry(REFUND_PENDING, Set.of(PARTIALLY_REFUNDED, FULLY_REFUNDED, CAPTURED)),
        Map.entry(PARTIALLY_REFUNDED, Set.of(REFUND_PENDING, FULLY_REFUNDED))
    );

    public boolean canTransitionTo(PaymentStatus target) {
        Set<PaymentStatus> allowed = TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    public boolean isTerminal() {
        return this == AUTH_FAILED || this == CAPTURE_FAILED ||
               this == FULLY_REFUNDED || this == CANCELLED;
    }

    public boolean canCapture() {
        return this == AUTHORIZED;
    }

    public boolean canRefund() {
        return this == CAPTURED || this == PARTIALLY_REFUNDED;
    }
}
