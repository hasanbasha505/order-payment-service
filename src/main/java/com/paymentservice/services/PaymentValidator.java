package com.paymentservice.services;

import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.exceptions.*;
import com.paymentservice.models.Payment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * Business rule validator for payment operations.
 */
@Component
public class PaymentValidator {

    private static final Set<PaymentStatus> REFUNDABLE_STATUSES = Set.of(
        PaymentStatus.CAPTURED,
        PaymentStatus.PARTIALLY_REFUNDED
    );

    /**
     * Validate that payment can be authorized.
     */
    public void validateAuthorization(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(payment.getStatus(), "authorize");
        }
    }

    /**
     * Validate that payment can be captured.
     */
    public void validateCapture(Payment payment, long captureAmount) {
        // Rule 1: Must be authorized
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new InvalidPaymentStateException(
                String.format("Payment must be AUTHORIZED before capture. Current: %s",
                    payment.getStatus()));
        }

        // Rule 2: Authorization not expired
        if (payment.getExpiresAt() != null && Instant.now().isAfter(payment.getExpiresAt())) {
            throw new AuthorizationExpiredException(payment.getId(), payment.getExpiresAt());
        }

        // Rule 3: Amount cannot exceed authorized amount
        if (payment.getAuthorizedAmount() != null && captureAmount > payment.getAuthorizedAmount()) {
            throw new InvalidPaymentStateException(
                String.format("Capture amount %d exceeds authorized amount %d",
                    captureAmount, payment.getAuthorizedAmount()));
        }

        // Rule 4: Prevent double capture (defense in depth)
        if (payment.getCapturedAmount() > 0) {
            throw new DuplicateCaptureException(payment.getId(), payment.getCapturedAmount());
        }
    }

    /**
     * Validate that payment can be refunded.
     */
    public void validateRefund(Payment payment, long refundAmount) {
        // Rule 1: Must be in refundable state
        if (!REFUNDABLE_STATUSES.contains(payment.getStatus())) {
            throw new InvalidPaymentStateException(payment.getStatus(), "refund");
        }

        // Rule 2: Amount must be positive
        if (refundAmount <= 0) {
            throw new InvalidPaymentStateException("Refund amount must be positive");
        }

        // Rule 3: Refund cannot exceed available balance
        long availableForRefund = payment.getAvailableForRefund();
        if (refundAmount > availableForRefund) {
            throw new InsufficientRefundBalanceException(refundAmount, availableForRefund);
        }
    }

    /**
     * Validate that authorization can be voided.
     */
    public void validateVoid(Payment payment) {
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new InvalidPaymentStateException(payment.getStatus(), "void");
        }
    }
}
