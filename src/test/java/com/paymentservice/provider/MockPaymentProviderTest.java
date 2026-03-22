package com.paymentservice.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockPaymentProvider")
class MockPaymentProviderTest {

    private MockPaymentProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockPaymentProvider();
        provider.clearProcessedKeys();
    }

    @Nested
    @DisplayName("authorize")
    class AuthorizeTests {

        @Test
        @DisplayName("should return successful authorization")
        void shouldReturnSuccessfulAuthorization() {
            ProviderResponse response = provider.authorize(
                    UUID.randomUUID(), 2500L, "USD", "CARD", "tok_valid", "auth-key-1");

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getTransactionId()).isNotNull();
            assertThat(response.getAuthorizationCode()).isNotNull();
        }

        @Test
        @DisplayName("should fail for declined card token")
        void shouldFailForDeclinedCardToken() {
            ProviderResponse response = provider.authorize(
                    UUID.randomUUID(), 2500L, "USD", "CARD", "tok_decline", "auth-key-2");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isNotNull();
        }

        @Test
        @DisplayName("should return idempotent response for same key")
        void shouldReturnIdempotentResponse() {
            String idempotencyKey = "auth-key-idem";

            ProviderResponse first = provider.authorize(
                    UUID.randomUUID(), 2500L, "USD", "CARD", "tok_valid", idempotencyKey);

            ProviderResponse second = provider.authorize(
                    UUID.randomUUID(), 2500L, "USD", "CARD", "tok_valid", idempotencyKey);

            assertThat(first.getTransactionId()).isEqualTo(second.getTransactionId());
        }
    }

    @Nested
    @DisplayName("capture")
    class CaptureTests {

        @Test
        @DisplayName("should return successful capture")
        void shouldReturnSuccessfulCapture() {
            ProviderResponse response = provider.capture("pi_123456", 2500L, "capture-key-1");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getTransactionId()).isNotNull();
        }

        @Test
        @DisplayName("should return idempotent response for same key")
        void shouldReturnIdempotentResponse() {
            String idempotencyKey = "capture-key-idem";

            ProviderResponse first = provider.capture("pi_123456", 2500L, idempotencyKey);
            ProviderResponse second = provider.capture("pi_123456", 2500L, idempotencyKey);

            assertThat(first.getTransactionId()).isEqualTo(second.getTransactionId());
        }
    }

    @Nested
    @DisplayName("refund")
    class RefundTests {

        @Test
        @DisplayName("should return successful refund")
        void shouldReturnSuccessfulRefund() {
            ProviderResponse response = provider.refund("ch_123456", 1000L, "refund-key-1");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getTransactionId()).isNotNull();
        }

        @Test
        @DisplayName("should return idempotent response for same key")
        void shouldReturnIdempotentResponse() {
            String idempotencyKey = "refund-key-idem";

            ProviderResponse first = provider.refund("ch_123456", 1000L, idempotencyKey);
            ProviderResponse second = provider.refund("ch_123456", 1000L, idempotencyKey);

            assertThat(first.getTransactionId()).isEqualTo(second.getTransactionId());
        }
    }

    @Nested
    @DisplayName("healthCheck")
    class HealthCheckTests {

        @Test
        @DisplayName("should return true when healthy")
        void shouldReturnTrueWhenHealthy() {
            assertThat(provider.healthCheck()).isTrue();
        }
    }

    @Nested
    @DisplayName("failure simulation")
    class FailureSimulationTests {

        @Test
        @DisplayName("should fail authorization when failure rate is 1.0")
        void shouldFailAuthorizationWithHighFailureRate() {
            provider.setAuthorizationFailureRate(1.0);

            ProviderResponse response = provider.authorize(
                    UUID.randomUUID(), 2500L, "USD", "CARD", "tok_valid", "auth-fail-key");

            assertThat(response.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("should fail capture when failure rate is 1.0")
        void shouldFailCaptureWithHighFailureRate() {
            provider.setCaptureFailureRate(1.0);

            ProviderResponse response = provider.capture("pi_123456", 2500L, "capture-fail-key");

            assertThat(response.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("should fail refund when failure rate is 1.0")
        void shouldFailRefundWithHighFailureRate() {
            provider.setRefundFailureRate(1.0);

            ProviderResponse response = provider.refund("ch_123456", 1000L, "refund-fail-key");

            assertThat(response.isSuccess()).isFalse();
        }
    }
}
