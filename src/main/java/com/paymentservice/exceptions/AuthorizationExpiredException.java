package com.paymentservice.exceptions;

import java.time.Instant;
import java.util.UUID;

public class AuthorizationExpiredException extends PaymentException {

    public AuthorizationExpiredException(UUID paymentId, Instant expiredAt) {
        super("AUTHORIZATION_EXPIRED",
            String.format("Authorization for payment %s expired at %s", paymentId, expiredAt));
    }
}
