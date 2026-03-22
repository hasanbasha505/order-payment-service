package com.paymentservice.exceptions;

import com.paymentservice.enums.PaymentStatus;

public class InvalidPaymentStateException extends PaymentException {

    public InvalidPaymentStateException(String message) {
        super("INVALID_PAYMENT_STATE", message);
    }

    public InvalidPaymentStateException(PaymentStatus currentStatus, String operation) {
        super("INVALID_PAYMENT_STATE",
            String.format("Cannot %s payment in status: %s", operation, currentStatus));
    }
}
