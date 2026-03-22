package com.paymentservice.exceptions;

import java.util.UUID;

public class DuplicateCaptureException extends PaymentException {

    public DuplicateCaptureException(UUID paymentId, Long existingAmount) {
        super("DUPLICATE_CAPTURE",
            String.format("Payment %s already captured with amount: %d", paymentId, existingAmount));
    }
}
