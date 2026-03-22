package com.paymentservice.services;

import com.paymentservice.dto.request.AuthorizePaymentRequest;
import com.paymentservice.dto.request.CapturePaymentRequest;
import com.paymentservice.dto.request.RefundRequest;
import com.paymentservice.dto.response.PaymentResponse;

import java.util.UUID;

/**
 * Payment Service Interface.
 *
 * Provides two sets of methods:
 * 1. By Payment ID - for internal use and backward compatibility
 * 2. By Order ID - for RESTful API (payments as child resources of orders)
 */
public interface PaymentService {

    // ==================== Methods by Payment ID (Internal Use) ====================

    PaymentResponse authorizePayment(UUID paymentId, AuthorizePaymentRequest request, String idempotencyKey);

    PaymentResponse capturePayment(UUID paymentId, CapturePaymentRequest request, String idempotencyKey);

    PaymentResponse refundPayment(UUID paymentId, RefundRequest request, String idempotencyKey);

    PaymentResponse getPayment(UUID paymentId);

    PaymentResponse getPaymentWithTransactions(UUID paymentId);

    // ==================== Methods by Order ID (RESTful API) ====================

    /**
     * Authorize payment by order ID.
     * Looks up the payment associated with the order and authorizes it.
     *
     * @param orderId the order ID
     * @param request the authorization request containing amount and payment method
     * @param idempotencyKey unique key for idempotent processing
     * @return payment response with authorization details
     */
    PaymentResponse authorizePaymentByOrderId(UUID orderId, AuthorizePaymentRequest request, String idempotencyKey);

    /**
     * Capture payment by order ID.
     * Captures the previously authorized payment associated with the order.
     *
     * @param orderId the order ID
     * @param request the capture request (optional amount for partial capture)
     * @param idempotencyKey unique key for idempotent processing
     * @return payment response with capture details
     */
    PaymentResponse capturePaymentByOrderId(UUID orderId, CapturePaymentRequest request, String idempotencyKey);

    /**
     * Refund payment by order ID.
     * Refunds the captured payment associated with the order.
     *
     * @param orderId the order ID
     * @param request the refund request containing amount and reason
     * @param idempotencyKey unique key for idempotent processing
     * @return payment response with refund details
     */
    PaymentResponse refundPaymentByOrderId(UUID orderId, RefundRequest request, String idempotencyKey);

    /**
     * Get payment by order ID.
     *
     * @param orderId the order ID
     * @return payment response
     */
    PaymentResponse getPaymentByOrderId(UUID orderId);

    /**
     * Get payment with transaction history by order ID.
     *
     * @param orderId the order ID
     * @return payment response with all transactions
     */
    PaymentResponse getPaymentWithTransactionsByOrderId(UUID orderId);
}
