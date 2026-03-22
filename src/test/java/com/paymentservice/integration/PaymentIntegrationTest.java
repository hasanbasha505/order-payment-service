package com.paymentservice.integration;

import com.paymentservice.dto.request.AuthorizePaymentRequest;
import com.paymentservice.dto.request.CapturePaymentRequest;
import com.paymentservice.dto.request.CreateOrderRequest;
import com.paymentservice.dto.request.RefundRequest;
import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.services.OrderService;
import com.paymentservice.services.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Payment Integration Tests")
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_payments")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    private UUID testRestaurantId;

    @BeforeEach
    void setUp() {
        // This restaurant ID should exist from the seed data
        testRestaurantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    }

    @Test
    @DisplayName("should complete full payment lifecycle: authorize -> capture")
    void shouldCompleteFullPaymentLifecycle() {
        // Create order
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(2500L)
                .currencyCode("USD")
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest, "order-" + UUID.randomUUID());
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.getPayment()).isNotNull();

        UUID paymentId = orderResponse.getPayment().getPaymentId();

        // Authorize
        AuthorizePaymentRequest authRequest = AuthorizePaymentRequest.builder()
                .amount(2500L)
                .paymentMethod("CARD")
                .paymentToken("tok_valid")
                .build();

        PaymentResponse authResponse = paymentService.authorizePayment(
                paymentId, authRequest, "auth-" + UUID.randomUUID());

        assertThat(authResponse.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(authResponse.getAuthorizedAmount()).isEqualTo(2500L);

        // Capture
        CapturePaymentRequest captureRequest = CapturePaymentRequest.builder()
                .amount(2500L)
                .build();

        PaymentResponse captureResponse = paymentService.capturePayment(
                paymentId, captureRequest, "capture-" + UUID.randomUUID());

        assertThat(captureResponse.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(captureResponse.getCapturedAmount()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("should handle partial refund")
    void shouldHandlePartialRefund() {
        // Create and complete payment
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(5000L)
                .currencyCode("USD")
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest, "order-" + UUID.randomUUID());
        UUID paymentId = orderResponse.getPayment().getPaymentId();

        // Authorize
        paymentService.authorizePayment(paymentId,
                AuthorizePaymentRequest.builder()
                        .amount(5000L)
                        .paymentMethod("CARD")
                        .paymentToken("tok_valid")
                        .build(),
                "auth-" + UUID.randomUUID());

        // Capture
        paymentService.capturePayment(paymentId,
                CapturePaymentRequest.builder().amount(5000L).build(),
                "capture-" + UUID.randomUUID());

        // Partial refund
        RefundRequest refundRequest = RefundRequest.builder()
                .amount(2000L)
                .reason("Customer complaint")
                .build();

        PaymentResponse refundResponse = paymentService.refundPayment(
                paymentId, refundRequest, "refund-" + UUID.randomUUID());

        assertThat(refundResponse.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(refundResponse.getRefundedAmount()).isEqualTo(2000L);
        assertThat(refundResponse.getCapturedAmount() - refundResponse.getRefundedAmount()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("should return idempotent response for duplicate capture")
    void shouldReturnIdempotentResponseForDuplicateCapture() {
        // Create and authorize order
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(2500L)
                .currencyCode("USD")
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest, "order-" + UUID.randomUUID());
        UUID paymentId = orderResponse.getPayment().getPaymentId();

        paymentService.authorizePayment(paymentId,
                AuthorizePaymentRequest.builder()
                        .amount(2500L)
                        .paymentMethod("CARD")
                        .paymentToken("tok_valid")
                        .build(),
                "auth-" + UUID.randomUUID());

        // Capture with same idempotency key
        String idempotencyKey = "capture-" + UUID.randomUUID();
        CapturePaymentRequest captureRequest = CapturePaymentRequest.builder().amount(2500L).build();

        PaymentResponse firstCapture = paymentService.capturePayment(paymentId, captureRequest, idempotencyKey);
        PaymentResponse secondCapture = paymentService.capturePayment(paymentId, captureRequest, idempotencyKey);

        // Both should return same result
        assertThat(secondCapture.getStatus()).isEqualTo(firstCapture.getStatus());
        assertThat(secondCapture.getCapturedAmount()).isEqualTo(firstCapture.getCapturedAmount());
    }
}
