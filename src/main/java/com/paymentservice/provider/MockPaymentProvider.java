package com.paymentservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock payment provider for testing and development.
 * Simulates various payment scenarios including failures.
 */
@Slf4j
@Component
public class MockPaymentProvider implements PaymentProviderGateway {

    private final Map<String, String> processedIdempotencyKeys = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // Configurable failure rates for testing
    private double authorizationFailureRate = 0.0;
    private double captureFailureRate = 0.0;
    private double refundFailureRate = 0.0;
    private double timeoutRate = 0.0;

    @Override
    public ProviderResponse authorize(
            UUID paymentId,
            long amount,
            String currencyCode,
            String paymentMethod,
            String paymentToken,
            String idempotencyKey) {

        log.info("Mock authorize: paymentId={}, amount={}, method={}, idempotencyKey={}",
            paymentId, amount, paymentMethod, idempotencyKey);

        // Idempotency check
        String existingTxnId = processedIdempotencyKeys.get(idempotencyKey);
        if (existingTxnId != null) {
            log.info("Returning idempotent response for key: {}", idempotencyKey);
            return ProviderResponse.success(existingTxnId, "AUTH_" + existingTxnId.substring(0, 8));
        }

        // Simulate timeout
        if (shouldTimeout()) {
            log.warn("Simulated timeout for authorization");
            return ProviderResponse.timeout("Provider did not respond in time");
        }

        // Simulate failure
        if (shouldFailAuthorization(paymentToken)) {
            log.info("Simulated authorization failure");
            return ProviderResponse.failure("CARD_DECLINED", "Card was declined by the bank");
        }

        // Simulate success
        String transactionId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String authCode = "AUTH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        processedIdempotencyKeys.put(idempotencyKey, transactionId);

        return ProviderResponse.builder()
            .success(true)
            .transactionId(transactionId)
            .authorizationCode(authCode)
            .rawResponse(Map.of(
                "id", transactionId,
                "status", "requires_capture",
                "amount", amount,
                "currency", currencyCode
            ))
            .build();
    }

    @Override
    public ProviderResponse capture(String providerPaymentId, long amount, String idempotencyKey) {
        log.info("Mock capture: providerPaymentId={}, amount={}, idempotencyKey={}",
            providerPaymentId, amount, idempotencyKey);

        // Idempotency check
        String existingTxnId = processedIdempotencyKeys.get(idempotencyKey);
        if (existingTxnId != null) {
            log.info("Returning idempotent response for capture key: {}", idempotencyKey);
            return ProviderResponse.success(existingTxnId);
        }

        // Simulate timeout
        if (shouldTimeout()) {
            log.warn("Simulated timeout for capture");
            return ProviderResponse.timeout("Provider did not respond in time");
        }

        // Simulate failure
        if (shouldFail(captureFailureRate)) {
            log.info("Simulated capture failure");
            return ProviderResponse.failure("CAPTURE_FAILED", "Capture failed - authorization may have expired");
        }

        // Simulate success
        String captureId = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        processedIdempotencyKeys.put(idempotencyKey, captureId);

        return ProviderResponse.builder()
            .success(true)
            .transactionId(captureId)
            .rawResponse(Map.of(
                "id", captureId,
                "payment_intent", providerPaymentId,
                "status", "succeeded",
                "amount", amount
            ))
            .build();
    }

    @Override
    public ProviderResponse refund(String providerPaymentId, long amount, String idempotencyKey) {
        log.info("Mock refund: providerPaymentId={}, amount={}, idempotencyKey={}",
            providerPaymentId, amount, idempotencyKey);

        // Idempotency check
        String existingTxnId = processedIdempotencyKeys.get(idempotencyKey);
        if (existingTxnId != null) {
            log.info("Returning idempotent response for refund key: {}", idempotencyKey);
            return ProviderResponse.success(existingTxnId);
        }

        // Simulate timeout
        if (shouldTimeout()) {
            log.warn("Simulated timeout for refund");
            return ProviderResponse.timeout("Provider did not respond in time");
        }

        // Simulate failure
        if (shouldFail(refundFailureRate)) {
            log.info("Simulated refund failure");
            return ProviderResponse.failure("REFUND_FAILED", "Refund failed - funds may not be available");
        }

        // Simulate success
        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        processedIdempotencyKeys.put(idempotencyKey, refundId);

        return ProviderResponse.builder()
            .success(true)
            .transactionId(refundId)
            .rawResponse(Map.of(
                "id", refundId,
                "payment_intent", providerPaymentId,
                "status", "succeeded",
                "amount", amount
            ))
            .build();
    }

    @Override
    public ProviderResponse voidAuthorization(String providerPaymentId, String idempotencyKey) {
        log.info("Mock void: providerPaymentId={}, idempotencyKey={}",
            providerPaymentId, idempotencyKey);

        String voidId = "void_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return ProviderResponse.success(voidId);
    }

    @Override
    public boolean healthCheck() {
        return true;
    }

    // Configuration methods for testing

    public void setAuthorizationFailureRate(double rate) {
        this.authorizationFailureRate = rate;
    }

    public void setCaptureFailureRate(double rate) {
        this.captureFailureRate = rate;
    }

    public void setRefundFailureRate(double rate) {
        this.refundFailureRate = rate;
    }

    public void setTimeoutRate(double rate) {
        this.timeoutRate = rate;
    }

    public void clearProcessedKeys() {
        processedIdempotencyKeys.clear();
    }

    private boolean shouldFailAuthorization(String paymentToken) {
        // Special test tokens for deterministic failure
        if (paymentToken != null) {
            if (paymentToken.contains("decline")) {
                return true;
            }
            if (paymentToken.contains("insufficient")) {
                return true;
            }
        }
        return shouldFail(authorizationFailureRate);
    }

    private boolean shouldFail(double rate) {
        return random.nextDouble() < rate;
    }

    private boolean shouldTimeout() {
        return random.nextDouble() < timeoutRate;
    }
}
