package com.paymentservice.exceptions;

public class IdempotencyConflictException extends PaymentException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("IDEMPOTENCY_CONFLICT",
            String.format("Idempotency key '%s' was used with a different request body", idempotencyKey));
    }
}
