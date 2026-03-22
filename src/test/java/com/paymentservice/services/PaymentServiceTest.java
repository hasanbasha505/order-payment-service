package com.paymentservice.services;

import com.paymentservice.dto.request.AuthorizePaymentRequest;
import com.paymentservice.dto.request.CapturePaymentRequest;
import com.paymentservice.dto.request.RefundRequest;
import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.enums.TransactionStatus;
import com.paymentservice.enums.TransactionType;
import com.paymentservice.exceptions.PaymentNotFoundException;
import com.paymentservice.models.Payment;
import com.paymentservice.models.PaymentTransaction;
import com.paymentservice.provider.PaymentProviderGateway;
import com.paymentservice.provider.ProviderResponse;
import com.paymentservice.repositories.PaymentRepository;
import com.paymentservice.repositories.PaymentTransactionRepository;
import com.paymentservice.services.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private PaymentProviderGateway paymentProvider;

    @Mock
    private PaymentValidator validator;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment testPayment;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        testPayment = new Payment();
        testPayment.setId(paymentId);
        testPayment.setOrderId(UUID.randomUUID());
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setCurrencyCode("USD");
        testPayment.setCapturedAmount(0L);
        testPayment.setRefundedAmount(0L);
    }

    @Nested
    @DisplayName("authorizePayment")
    class AuthorizePaymentTests {

        @Test
        @DisplayName("should authorize payment successfully")
        void shouldAuthorizePaymentSuccessfully() {
            AuthorizePaymentRequest request = AuthorizePaymentRequest.builder()
                    .amount(2500L)
                    .paymentMethod("CARD")
                    .paymentToken("tok_valid")
                    .build();

            when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(testPayment));
            when(transactionRepository.findByPaymentIdAndIdempotencyKey(any(), any()))
                    .thenReturn(Optional.empty());
            when(paymentProvider.authorize(any(UUID.class), anyLong(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(ProviderResponse.success("txn_123", "auth_123"));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.authorizePayment(paymentId, request, "auth-key-1");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(response.getAuthorizedAmount()).isEqualTo(2500L);

            verify(validator).validateAuthorization(testPayment);
            verify(paymentProvider).authorize(eq(paymentId), eq(2500L), eq("USD"), eq("CARD"), eq("tok_valid"), eq("auth-key-1"));
        }

        @Test
        @DisplayName("should return cached response for idempotent request")
        void shouldReturnCachedResponseForIdempotentRequest() {
            PaymentTransaction existingTxn = new PaymentTransaction();
            existingTxn.setId(UUID.randomUUID());
            existingTxn.setPaymentId(paymentId);
            existingTxn.setTransactionType(TransactionType.AUTHORIZE);
            existingTxn.setStatus(TransactionStatus.SUCCESS);
            existingTxn.setAmount(2500L);

            testPayment.setStatus(PaymentStatus.AUTHORIZED);
            testPayment.setAuthorizedAmount(2500L);

            when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(testPayment));
            when(transactionRepository.findByPaymentIdAndIdempotencyKey(paymentId, "auth-key-1"))
                    .thenReturn(Optional.of(existingTxn));

            AuthorizePaymentRequest request = AuthorizePaymentRequest.builder()
                    .amount(2500L)
                    .paymentMethod("CARD")
                    .paymentToken("tok_valid")
                    .build();

            PaymentResponse response = paymentService.authorizePayment(paymentId, request, "auth-key-1");

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
            verify(paymentProvider, never()).authorize(any(), anyLong(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.empty());

            AuthorizePaymentRequest request = AuthorizePaymentRequest.builder()
                    .amount(2500L)
                    .paymentMethod("CARD")
                    .paymentToken("tok_valid")
                    .build();

            assertThatThrownBy(() -> paymentService.authorizePayment(paymentId, request, "key"))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("capturePayment")
    class CapturePaymentTests {

        @Test
        @DisplayName("should capture payment successfully")
        void shouldCapturePaymentSuccessfully() {
            testPayment.setStatus(PaymentStatus.AUTHORIZED);
            testPayment.setAuthorizedAmount(2500L);
            testPayment.setProviderAuthCode("auth_123");
            testPayment.setProviderPaymentId("pi_123");
            testPayment.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

            CapturePaymentRequest request = CapturePaymentRequest.builder()
                    .amount(2500L)
                    .build();

            when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(testPayment));
            when(transactionRepository.findByPaymentIdAndIdempotencyKey(any(), any()))
                    .thenReturn(Optional.empty());
            when(paymentProvider.capture(anyString(), anyLong(), anyString()))
                    .thenReturn(ProviderResponse.success("txn_456", null));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.capturePayment(paymentId, request, "capture-key-1");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(response.getCapturedAmount()).isEqualTo(2500L);

            verify(validator).validateCapture(testPayment, 2500L);
        }

        @Test
        @DisplayName("should capture full amount when amount not specified")
        void shouldCaptureFullAmountWhenNotSpecified() {
            testPayment.setStatus(PaymentStatus.AUTHORIZED);
            testPayment.setAuthorizedAmount(2500L);
            testPayment.setProviderAuthCode("auth_123");
            testPayment.setProviderPaymentId("pi_123");
            testPayment.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

            CapturePaymentRequest request = new CapturePaymentRequest(); // No amount

            when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(testPayment));
            when(transactionRepository.findByPaymentIdAndIdempotencyKey(any(), any()))
                    .thenReturn(Optional.empty());
            when(paymentProvider.capture(anyString(), anyLong(), anyString()))
                    .thenReturn(ProviderResponse.success("txn_456", null));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.capturePayment(paymentId, request, "capture-key-1");

            verify(validator).validateCapture(testPayment, 2500L);
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPaymentTests {

        @Test
        @DisplayName("should process partial refund successfully")
        void shouldProcessPartialRefundSuccessfully() {
            testPayment.setStatus(PaymentStatus.CAPTURED);
            testPayment.setCapturedAmount(2500L);
            testPayment.setRefundedAmount(0L);
            testPayment.setProviderPaymentId("pay_123");

            RefundRequest request = RefundRequest.builder()
                    .amount(1000L)
                    .reason("Customer complaint")
                    .build();

            when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(testPayment));
            when(transactionRepository.findByPaymentIdAndIdempotencyKey(any(), any()))
                    .thenReturn(Optional.empty());
            when(paymentProvider.refund(anyString(), anyLong(), anyString()))
                    .thenReturn(ProviderResponse.success("txn_789", null));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.refundPayment(paymentId, request, "refund-key-1");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(response.getRefundedAmount()).isEqualTo(1000L);

            verify(validator).validateRefund(testPayment, 1000L);
        }

        @Test
        @DisplayName("should process full refund successfully")
        void shouldProcessFullRefundSuccessfully() {
            testPayment.setStatus(PaymentStatus.CAPTURED);
            testPayment.setCapturedAmount(2500L);
            testPayment.setRefundedAmount(0L);
            testPayment.setProviderPaymentId("pay_123");

            RefundRequest request = RefundRequest.builder()
                    .amount(2500L)
                    .reason("Full refund")
                    .build();

            when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(testPayment));
            when(transactionRepository.findByPaymentIdAndIdempotencyKey(any(), any()))
                    .thenReturn(Optional.empty());
            when(paymentProvider.refund(anyString(), anyLong(), anyString()))
                    .thenReturn(ProviderResponse.success("txn_789", null));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.refundPayment(paymentId, request, "refund-key-1");

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.FULLY_REFUNDED);
            assertThat(response.getRefundedAmount()).isEqualTo(2500L);
        }

        @Test
        @DisplayName("should handle provider failure gracefully")
        void shouldHandleProviderFailureGracefully() {
            testPayment.setStatus(PaymentStatus.CAPTURED);
            testPayment.setCapturedAmount(2500L);
            testPayment.setRefundedAmount(0L);
            testPayment.setProviderPaymentId("pay_123");

            RefundRequest request = RefundRequest.builder()
                    .amount(1000L)
                    .reason("Test")
                    .build();

            when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(testPayment));
            when(transactionRepository.findByPaymentIdAndIdempotencyKey(any(), any()))
                    .thenReturn(Optional.empty());
            when(paymentProvider.refund(anyString(), anyLong(), anyString()))
                    .thenReturn(ProviderResponse.failure("DECLINED", "Card declined"));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentResponse response = paymentService.refundPayment(paymentId, request, "refund-key-1");

            // Status should remain CAPTURED on failure
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(response.getRefundedAmount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPaymentTests {

        @Test
        @DisplayName("should return payment details")
        void shouldReturnPaymentDetails() {
            testPayment.setStatus(PaymentStatus.CAPTURED);
            testPayment.setAuthorizedAmount(2500L);
            testPayment.setCapturedAmount(2500L);

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

            PaymentResponse response = paymentService.getPayment(paymentId);

            assertThat(response).isNotNull();
            assertThat(response.getPaymentId()).isEqualTo(paymentId);
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        }

        @Test
        @DisplayName("should throw when payment not found")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }
}
