package com.paymentservice.services.impl;

import com.paymentservice.dto.request.AuthorizePaymentRequest;
import com.paymentservice.dto.request.CapturePaymentRequest;
import com.paymentservice.dto.request.RefundRequest;
import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.dto.response.TransactionResponse;
import com.paymentservice.enums.OutboxStatus;
import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.enums.TransactionStatus;
import com.paymentservice.enums.TransactionType;
import com.paymentservice.exceptions.PaymentNotFoundException;
import com.paymentservice.models.OutboxEvent;
import com.paymentservice.models.Payment;
import com.paymentservice.models.PaymentTransaction;
import com.paymentservice.provider.PaymentProviderGateway;
import com.paymentservice.provider.ProviderResponse;
import com.paymentservice.repositories.OutboxEventRepository;
import com.paymentservice.repositories.PaymentRepository;
import com.paymentservice.repositories.PaymentTransactionRepository;
import com.paymentservice.services.PaymentService;
import com.paymentservice.services.PaymentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment service implementation with production-grade concurrency handling.
 *
 * Key safety mechanisms:
 * 1. READ_COMMITTED isolation with pessimistic locking (SELECT FOR UPDATE)
 * 2. Idempotency check at service level
 * 3. Transactional outbox for event publishing
 * 4. Rate limiting at controller level
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final OutboxEventRepository outboxRepository;
    private final PaymentProviderGateway paymentProvider;
    private final PaymentValidator validator;

    private static final String PROVIDER_NAME = "MOCK_PROVIDER";
    private static final int AUTH_EXPIRY_DAYS = 7;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse authorizePayment(UUID paymentId, AuthorizePaymentRequest request, String idempotencyKey) {
        MDC.put("paymentId", paymentId.toString());
        MDC.put("idempotencyKey", idempotencyKey);
        MDC.put("operation", "AUTHORIZE");

        log.info("Processing authorization: amount={}, method={}", request.getAmount(), request.getPaymentMethod());

        try {
            // 1. Acquire pessimistic lock
            Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            // 2. Idempotency check
            Optional<PaymentTransaction> existing = transactionRepository
                .findByPaymentIdAndTransactionTypeAndIdempotencyKey(paymentId, TransactionType.AUTHORIZE, idempotencyKey);
            if (existing.isPresent()) {
                log.info("Returning idempotent response for authorization");
                return buildPaymentResponse(payment, existing.get());
            }

            // 3. Business validation
            validator.validateAuthorization(payment);

            // 4. Create INITIATED transaction record
            PaymentTransaction txn = createTransaction(payment, TransactionType.AUTHORIZE,
                request.getAmount(), idempotencyKey);

            // 5. Call payment provider (still inside transaction for simplicity in this implementation)
            ProviderResponse providerResponse = paymentProvider.authorize(
                paymentId,
                request.getAmount(),
                payment.getCurrencyCode(),
                request.getPaymentMethod(),
                request.getPaymentToken(),
                idempotencyKey
            );

            // 6. Update transaction with provider response
            updateTransactionFromProvider(txn, providerResponse);

            // 7. Update payment state
            if (providerResponse.isSuccess()) {
                payment.setStatus(PaymentStatus.AUTHORIZED);
                payment.setAuthorizedAmount(request.getAmount());
                payment.setPaymentMethod(request.getPaymentMethod());
                payment.setPaymentProvider(PROVIDER_NAME);
                payment.setProviderPaymentId(providerResponse.getTransactionId());
                payment.setProviderAuthCode(providerResponse.getAuthorizationCode());
                payment.setAuthorizedAt(Instant.now());
                payment.setExpiresAt(Instant.now().plus(AUTH_EXPIRY_DAYS, ChronoUnit.DAYS));
            } else {
                payment.setStatus(PaymentStatus.AUTH_FAILED);
                payment.setFailureReason(providerResponse.getErrorMessage());
            }

            // 8. Persist
            transactionRepository.save(txn);
            paymentRepository.save(payment);

            // 9. Write outbox event
            writeOutboxEvent(payment, txn, "PAYMENT_AUTHORIZED");

            log.info("Authorization completed: status={}, providerTxnId={}",
                payment.getStatus(), txn.getProviderTxnId());

            return buildPaymentResponse(payment, txn);

        } finally {
            MDC.clear();
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse capturePayment(UUID paymentId, CapturePaymentRequest request, String idempotencyKey) {
        MDC.put("paymentId", paymentId.toString());
        MDC.put("idempotencyKey", idempotencyKey);
        MDC.put("operation", "CAPTURE");

        log.info("Processing capture: requestedAmount={}", request.getAmount());

        try {
            // 1. Acquire pessimistic lock
            Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            // 2. Idempotency check
            Optional<PaymentTransaction> existing = transactionRepository
                .findByPaymentIdAndTransactionTypeAndIdempotencyKey(paymentId, TransactionType.CAPTURE, idempotencyKey);
            if (existing.isPresent()) {
                log.info("Returning idempotent response for capture");
                return buildPaymentResponse(payment, existing.get());
            }

            // 3. Determine capture amount
            Long captureAmount = request.getAmount() != null ?
                request.getAmount() : payment.getAuthorizedAmount();

            // 4. Business validation
            validator.validateCapture(payment, captureAmount);

            // 5. Create INITIATED transaction record
            PaymentTransaction txn = createTransaction(payment, TransactionType.CAPTURE,
                captureAmount, idempotencyKey);

            // 6. Call payment provider
            ProviderResponse providerResponse = paymentProvider.capture(
                payment.getProviderPaymentId(),
                captureAmount,
                idempotencyKey
            );

            // 7. Update transaction with provider response
            updateTransactionFromProvider(txn, providerResponse);

            // 8. Update payment state
            if (providerResponse.isSuccess()) {
                payment.setStatus(PaymentStatus.CAPTURED);
                payment.setCapturedAmount(captureAmount);
                payment.setCapturedAt(Instant.now());
            } else if (providerResponse.isTimeout()) {
                // Leave in CAPTURE_PENDING for retry
                payment.setStatus(PaymentStatus.CAPTURE_PENDING);
            } else {
                payment.setStatus(PaymentStatus.CAPTURE_FAILED);
                payment.setFailureReason(providerResponse.getErrorMessage());
            }

            // 9. Persist
            transactionRepository.save(txn);
            paymentRepository.save(payment);

            // 10. Write outbox event
            writeOutboxEvent(payment, txn, "PAYMENT_CAPTURED");

            log.info("Capture completed: status={}, capturedAmount={}",
                payment.getStatus(), payment.getCapturedAmount());

            return buildPaymentResponse(payment, txn);

        } finally {
            MDC.clear();
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse refundPayment(UUID paymentId, RefundRequest request, String idempotencyKey) {
        MDC.put("paymentId", paymentId.toString());
        MDC.put("idempotencyKey", idempotencyKey);
        MDC.put("operation", "REFUND");

        log.info("Processing refund: amount={}, reason={}", request.getAmount(), request.getReason());

        try {
            // 1. Acquire pessimistic lock
            Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            // 2. Idempotency check
            Optional<PaymentTransaction> existing = transactionRepository
                .findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey);
            if (existing.isPresent()) {
                log.info("Returning idempotent response for refund");
                return buildPaymentResponse(payment, existing.get());
            }

            // 3. Business validation
            validator.validateRefund(payment, request.getAmount());

            // 4. Create INITIATED transaction record
            PaymentTransaction txn = createTransaction(payment, TransactionType.REFUND,
                request.getAmount(), idempotencyKey);

            // 5. Call payment provider
            ProviderResponse providerResponse = paymentProvider.refund(
                payment.getProviderPaymentId(),
                request.getAmount(),
                idempotencyKey
            );

            // 6. Update transaction with provider response
            updateTransactionFromProvider(txn, providerResponse);

            // 7. Update payment state
            if (providerResponse.isSuccess()) {
                long newRefundedAmount = payment.getRefundedAmount() + request.getAmount();
                payment.setRefundedAmount(newRefundedAmount);

                if (newRefundedAmount >= payment.getCapturedAmount()) {
                    payment.setStatus(PaymentStatus.FULLY_REFUNDED);
                } else {
                    payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
                }
            } else if (providerResponse.isTimeout()) {
                payment.setStatus(PaymentStatus.REFUND_PENDING);
            }

            // 8. Persist
            transactionRepository.save(txn);
            paymentRepository.save(payment);

            // 9. Write outbox event
            writeOutboxEvent(payment, txn, "REFUND_PROCESSED");

            log.info("Refund completed: status={}, totalRefunded={}",
                payment.getStatus(), payment.getRefundedAmount());

            return buildPaymentResponse(payment, txn);

        } finally {
            MDC.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return buildPaymentResponse(payment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentWithTransactions(UUID paymentId) {
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        List<TransactionResponse> transactions = payment.getTransactions().stream()
            .map(this::toTransactionResponse)
            .collect(Collectors.toList());

        return PaymentResponse.builder()
            .paymentId(payment.getId())
            .orderId(payment.getOrderId())
            .status(payment.getStatus())
            .authorizedAmount(payment.getAuthorizedAmount())
            .capturedAmount(payment.getCapturedAmount())
            .refundedAmount(payment.getRefundedAmount())
            .netAmount(payment.getNetAmount())
            .availableForRefund(payment.getAvailableForRefund())
            .currencyCode(payment.getCurrencyCode())
            .paymentMethod(payment.getPaymentMethod())
            .paymentProvider(payment.getPaymentProvider())
            .authorizationCode(payment.getProviderAuthCode())
            .authorizedAt(payment.getAuthorizedAt())
            .capturedAt(payment.getCapturedAt())
            .expiresAt(payment.getExpiresAt())
            .transactions(transactions)
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }

    // ==================== Methods by Order ID (RESTful API) ====================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse authorizePaymentByOrderId(UUID orderId, AuthorizePaymentRequest request, String idempotencyKey) {
        Payment payment = paymentRepository.findByOrderIdWithLock(orderId)
            .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderId));
        return authorizePayment(payment.getId(), request, idempotencyKey);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse capturePaymentByOrderId(UUID orderId, CapturePaymentRequest request, String idempotencyKey) {
        Payment payment = paymentRepository.findByOrderIdWithLock(orderId)
            .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderId));
        return capturePayment(payment.getId(), request, idempotencyKey);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse refundPaymentByOrderId(UUID orderId, RefundRequest request, String idempotencyKey) {
        Payment payment = paymentRepository.findByOrderIdWithLock(orderId)
            .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderId));
        return refundPayment(payment.getId(), request, idempotencyKey);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderId));
        return buildPaymentResponse(payment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentWithTransactionsByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderId));
        return getPaymentWithTransactions(payment.getId());
    }

    // ==================== Private Helper Methods ====================

    private PaymentTransaction createTransaction(Payment payment, TransactionType type,
                                                   Long amount, String idempotencyKey) {
        return PaymentTransaction.builder()
            .paymentId(payment.getId())
            .transactionType(type)
            .amount(amount)
            .currencyCode(payment.getCurrencyCode())
            .status(TransactionStatus.INITIATED)
            .idempotencyKey(idempotencyKey)
            .build();
    }

    private void updateTransactionFromProvider(PaymentTransaction txn, ProviderResponse response) {
        if (response.isSuccess()) {
            txn.setStatus(TransactionStatus.SUCCESS);
        } else if (response.isTimeout()) {
            txn.setStatus(TransactionStatus.TIMEOUT);
        } else {
            txn.setStatus(TransactionStatus.FAILED);
        }

        txn.setProviderTxnId(response.getTransactionId());
        txn.setProviderResponse(response.getRawResponse());
        txn.setErrorCode(response.getErrorCode());
        txn.setErrorMessage(response.getErrorMessage());
    }

    private void writeOutboxEvent(Payment payment, PaymentTransaction txn, String eventType) {
        OutboxEvent event = OutboxEvent.builder()
            .aggregateType("PAYMENT")
            .aggregateId(payment.getId())
            .eventType(eventType)
            .status(OutboxStatus.PENDING)
            .payload(Map.of(
                "paymentId", payment.getId().toString(),
                "orderId", payment.getOrderId().toString(),
                "transactionId", txn.getId().toString(),
                "transactionType", txn.getTransactionType().name(),
                "amount", txn.getAmount(),
                "currencyCode", txn.getCurrencyCode(),
                "status", txn.getStatus().name(),
                "timestamp", Instant.now().toString()
            ))
            .build();

        outboxRepository.save(event);
    }

    private PaymentResponse buildPaymentResponse(Payment payment, PaymentTransaction txn) {
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
            .paymentId(payment.getId())
            .orderId(payment.getOrderId())
            .status(payment.getStatus())
            .authorizedAmount(payment.getAuthorizedAmount())
            .capturedAmount(payment.getCapturedAmount())
            .refundedAmount(payment.getRefundedAmount())
            .netAmount(payment.getNetAmount())
            .availableForRefund(payment.getAvailableForRefund())
            .currencyCode(payment.getCurrencyCode())
            .paymentMethod(payment.getPaymentMethod())
            .paymentProvider(payment.getPaymentProvider())
            .authorizationCode(payment.getProviderAuthCode())
            .authorizedAt(payment.getAuthorizedAt())
            .capturedAt(payment.getCapturedAt())
            .expiresAt(payment.getExpiresAt())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt());

        if (txn != null) {
            builder.transactions(List.of(toTransactionResponse(txn)));
        }

        return builder.build();
    }

    private TransactionResponse toTransactionResponse(PaymentTransaction txn) {
        return TransactionResponse.builder()
            .transactionId(txn.getId())
            .type(txn.getTransactionType())
            .amount(txn.getAmount())
            .currencyCode(txn.getCurrencyCode())
            .status(txn.getStatus())
            .providerTransactionId(txn.getProviderTxnId())
            .errorCode(txn.getErrorCode())
            .errorMessage(txn.getErrorMessage())
            .createdAt(txn.getCreatedAt())
            .build();
    }
}
