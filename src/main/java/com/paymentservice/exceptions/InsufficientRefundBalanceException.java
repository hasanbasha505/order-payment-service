package com.paymentservice.exceptions;

public class InsufficientRefundBalanceException extends PaymentException {

    public InsufficientRefundBalanceException(Long requestedAmount, Long availableAmount) {
        super("INSUFFICIENT_REFUND_BALANCE",
            String.format("Refund amount %d exceeds available balance %d",
                requestedAmount, availableAmount));
    }
}
