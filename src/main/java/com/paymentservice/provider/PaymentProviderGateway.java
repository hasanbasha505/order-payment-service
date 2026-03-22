package com.paymentservice.provider;

import java.util.UUID;

/**
 * Payment provider gateway interface.
 * Abstracts communication with external payment providers (Stripe, Razorpay, etc.)
 */
public interface PaymentProviderGateway {

    /**
     * Authorize a payment.
     *
     * @param paymentId Internal payment ID
     * @param amount Amount in smallest currency unit (cents)
     * @param currencyCode ISO currency code
     * @param paymentMethod Payment method (CARD, UPI, etc.)
     * @param paymentToken Token from payment provider
     * @param idempotencyKey Idempotency key for the request
     * @return Provider response with authorization result
     */
    ProviderResponse authorize(
        UUID paymentId,
        long amount,
        String currencyCode,
        String paymentMethod,
        String paymentToken,
        String idempotencyKey
    );

    /**
     * Capture a previously authorized payment.
     *
     * @param providerPaymentId External payment ID from authorization
     * @param amount Amount to capture
     * @param idempotencyKey Idempotency key for the request
     * @return Provider response with capture result
     */
    ProviderResponse capture(
        String providerPaymentId,
        long amount,
        String idempotencyKey
    );

    /**
     * Refund a captured payment.
     *
     * @param providerPaymentId External payment ID
     * @param amount Amount to refund
     * @param idempotencyKey Idempotency key for the request
     * @return Provider response with refund result
     */
    ProviderResponse refund(
        String providerPaymentId,
        long amount,
        String idempotencyKey
    );

    /**
     * Void an authorization (cancel before capture).
     *
     * @param providerPaymentId External payment ID
     * @param idempotencyKey Idempotency key for the request
     * @return Provider response with void result
     */
    ProviderResponse voidAuthorization(
        String providerPaymentId,
        String idempotencyKey
    );

    /**
     * Health check for the provider.
     *
     * @return true if provider is healthy
     */
    boolean healthCheck();
}
