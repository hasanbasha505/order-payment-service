package com.paymentservice.services;

import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.exceptions.*;
import com.paymentservice.models.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("PaymentValidator")
class PaymentValidatorTest {

    private PaymentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PaymentValidator();
    }

    @Nested
    @DisplayName("validateAuthorization")
    class ValidateAuthorizationTests {

        @Test
        @DisplayName("should allow authorization when status is PENDING")
        void shouldAllowAuthorizationWhenPending() {
            Payment payment = createPayment(PaymentStatus.PENDING);

            assertThatCode(() -> validator.validateAuthorization(payment))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject authorization when already authorized")
        void shouldRejectWhenAlreadyAuthorized() {
            Payment payment = createPayment(PaymentStatus.AUTHORIZED);

            assertThatThrownBy(() -> validator.validateAuthorization(payment))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("AUTHORIZED");
        }

        @Test
        @DisplayName("should reject authorization when captured")
        void shouldRejectWhenCaptured() {
            Payment payment = createPayment(PaymentStatus.CAPTURED);

            assertThatThrownBy(() -> validator.validateAuthorization(payment))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }
    }

    @Nested
    @DisplayName("validateCapture")
    class ValidateCaptureTests {

        @Test
        @DisplayName("should allow capture when authorized and not expired")
        void shouldAllowCaptureWhenAuthorized() {
            Payment payment = createPayment(PaymentStatus.AUTHORIZED);
            payment.setAuthorizedAmount(2500L);
            payment.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

            assertThatCode(() -> validator.validateCapture(payment, 2500L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject capture when not authorized")
        void shouldRejectCaptureWhenNotAuthorized() {
            Payment payment = createPayment(PaymentStatus.PENDING);

            assertThatThrownBy(() -> validator.validateCapture(payment, 2500L))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }

        @Test
        @DisplayName("should reject capture when authorization expired")
        void shouldRejectWhenAuthorizationExpired() {
            Payment payment = createPayment(PaymentStatus.AUTHORIZED);
            payment.setAuthorizedAmount(2500L);
            payment.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

            assertThatThrownBy(() -> validator.validateCapture(payment, 2500L))
                    .isInstanceOf(AuthorizationExpiredException.class);
        }

        @Test
        @DisplayName("should reject capture when amount exceeds authorized")
        void shouldRejectWhenAmountExceedsAuthorized() {
            Payment payment = createPayment(PaymentStatus.AUTHORIZED);
            payment.setAuthorizedAmount(2500L);
            payment.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

            assertThatThrownBy(() -> validator.validateCapture(payment, 3000L))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("exceeds");
        }

        @Test
        @DisplayName("should reject double capture")
        void shouldRejectDoubleCapture() {
            Payment payment = createPayment(PaymentStatus.AUTHORIZED);
            payment.setAuthorizedAmount(2500L);
            payment.setCapturedAmount(2500L);
            payment.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

            assertThatThrownBy(() -> validator.validateCapture(payment, 2500L))
                    .isInstanceOf(DuplicateCaptureException.class);
        }
    }

    @Nested
    @DisplayName("validateRefund")
    class ValidateRefundTests {

        @Test
        @DisplayName("should allow refund when captured")
        void shouldAllowRefundWhenCaptured() {
            Payment payment = createPayment(PaymentStatus.CAPTURED);
            payment.setCapturedAmount(2500L);
            payment.setRefundedAmount(0L);

            assertThatCode(() -> validator.validateRefund(payment, 1000L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should allow refund when partially refunded")
        void shouldAllowRefundWhenPartiallyRefunded() {
            Payment payment = createPayment(PaymentStatus.PARTIALLY_REFUNDED);
            payment.setCapturedAmount(2500L);
            payment.setRefundedAmount(1000L);

            assertThatCode(() -> validator.validateRefund(payment, 500L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject refund when not captured")
        void shouldRejectRefundWhenNotCaptured() {
            Payment payment = createPayment(PaymentStatus.AUTHORIZED);

            assertThatThrownBy(() -> validator.validateRefund(payment, 1000L))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }

        @Test
        @DisplayName("should reject refund when amount exceeds available")
        void shouldRejectWhenAmountExceedsAvailable() {
            Payment payment = createPayment(PaymentStatus.CAPTURED);
            payment.setCapturedAmount(2500L);
            payment.setRefundedAmount(2000L);

            assertThatThrownBy(() -> validator.validateRefund(payment, 1000L))
                    .isInstanceOf(InsufficientRefundBalanceException.class);
        }

        @Test
        @DisplayName("should reject refund of zero or negative amount")
        void shouldRejectZeroOrNegativeAmount() {
            Payment payment = createPayment(PaymentStatus.CAPTURED);
            payment.setCapturedAmount(2500L);
            payment.setRefundedAmount(0L);

            assertThatThrownBy(() -> validator.validateRefund(payment, 0L))
                    .isInstanceOf(InvalidPaymentStateException.class);

            assertThatThrownBy(() -> validator.validateRefund(payment, -100L))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }
    }

    private Payment createPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setStatus(status);
        payment.setCurrencyCode("USD");
        payment.setCapturedAmount(0L);
        payment.setRefundedAmount(0L);
        return payment;
    }
}
