package com.paymentservice.exceptions;

import java.util.UUID;

public class PaymentNotFoundException extends PaymentException {

    public PaymentNotFoundException(UUID paymentId) {
        super("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId);
    }

    public PaymentNotFoundException(String message) {
        super("PAYMENT_NOT_FOUND", message);
    }
}
