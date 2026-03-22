package com.paymentservice.controllers;

import com.paymentservice.dto.request.AuthorizePaymentRequest;
import com.paymentservice.dto.request.CapturePaymentRequest;
import com.paymentservice.dto.request.RefundRequest;
import com.paymentservice.dto.response.ApiResponse;
import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.services.PaymentService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Payment Controller - RESTful endpoints for payment lifecycle management.
 *
 * Payments are child resources of Orders, following REST best practices:
 * - URL structure reflects parent-child relationship: /orders/{orderId}/payment
 * - Payment is a singular resource (1:1 with Order)
 * - All operations are accessed through the parent Order ID
 *
 * Extends BaseController for common response helper methods.
 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/payment")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment lifecycle management endpoints - child resource of Orders")
@RateLimiter(name = "paymentApi")
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    @PostMapping("/authorize")
    @Operation(summary = "Authorize payment",
               description = "Authorizes the payment for an order. Requires Idempotency-Key header.")
    public ResponseEntity<ApiResponse<PaymentResponse>> authorizePayment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AuthorizePaymentRequest request) {

        PaymentResponse response = paymentService.authorizePaymentByOrderId(orderId, request, idempotencyKey);
        return ok(response, "Payment authorized successfully");
    }

    @PostMapping("/capture")
    @Operation(summary = "Capture payment",
               description = "Captures a previously authorized payment. Prevents double capture.")
    public ResponseEntity<ApiResponse<PaymentResponse>> capturePayment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) CapturePaymentRequest request) {

        if (request == null) {
            request = new CapturePaymentRequest();
        }
        PaymentResponse response = paymentService.capturePaymentByOrderId(orderId, request, idempotencyKey);
        return ok(response, "Payment captured successfully");
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund payment",
               description = "Refunds a captured payment. Supports partial refunds.")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RefundRequest request) {

        PaymentResponse response = paymentService.refundPaymentByOrderId(orderId, request, idempotencyKey);
        return ok(response, "Refund processed successfully");
    }

    @GetMapping
    @Operation(summary = "Get payment", description = "Retrieves payment details for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get payment with transactions",
               description = "Retrieves payment with full transaction history")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentWithTransactions(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId) {

        PaymentResponse response = paymentService.getPaymentWithTransactionsByOrderId(orderId);
        return ok(response);
    }
}
