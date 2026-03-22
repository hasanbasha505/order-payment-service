package com.paymentservice.enums;

import java.util.Map;
import java.util.Set;

/**
 * Order status representing the order lifecycle.
 */
public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_AUTHORIZED,
    PAYMENT_CAPTURED,
    COMPLETED,
    CANCELLED,
    REFUNDED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        CREATED, Set.of(PAYMENT_PENDING, CANCELLED),
        PAYMENT_PENDING, Set.of(PAYMENT_AUTHORIZED, CANCELLED),
        PAYMENT_AUTHORIZED, Set.of(PAYMENT_CAPTURED, CANCELLED),
        PAYMENT_CAPTURED, Set.of(COMPLETED, REFUNDED),
        COMPLETED, Set.of(REFUNDED)
    );

    public boolean canTransitionTo(OrderStatus target) {
        Set<OrderStatus> allowed = TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }
}
