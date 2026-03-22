package com.paymentservice.concurrency;

import com.paymentservice.dto.request.AuthorizePaymentRequest;
import com.paymentservice.dto.request.CapturePaymentRequest;
import com.paymentservice.dto.request.CreateOrderRequest;
import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.exceptions.DuplicateCaptureException;
import com.paymentservice.exceptions.InvalidPaymentStateException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Concurrent Capture Tests")
class ConcurrentCaptureTest {

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
        testRestaurantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    }

    @Test
    @DisplayName("should prevent double capture under concurrent requests")
    void shouldPreventDoubleCaptureUnderConcurrentRequests() throws Exception {
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

        // Attempt concurrent captures
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    CapturePaymentRequest captureRequest = CapturePaymentRequest.builder()
                            .amount(2500L)
                            .build();

                    // Each thread uses unique idempotency key to simulate different requests
                    paymentService.capturePayment(paymentId, captureRequest, "capture-" + threadNum + "-" + UUID.randomUUID());
                    successCount.incrementAndGet();
                } catch (DuplicateCaptureException | InvalidPaymentStateException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        // Start all threads simultaneously
        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Only ONE capture should succeed
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(numThreads - 1);

        // Verify final state
        PaymentResponse finalState = paymentService.getPayment(paymentId);
        assertThat(finalState.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(finalState.getCapturedAmount()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("should handle idempotent capture requests safely")
    void shouldHandleIdempotentCaptureRequestsSafely() throws Exception {
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

        // Same idempotency key for all requests (simulating retries)
        String idempotencyKey = "capture-" + UUID.randomUUID();

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<PaymentResponse>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                CapturePaymentRequest captureRequest = CapturePaymentRequest.builder()
                        .amount(2500L)
                        .build();
                return paymentService.capturePayment(paymentId, captureRequest, idempotencyKey);
            }));
        }

        startLatch.countDown();

        List<PaymentResponse> responses = new ArrayList<>();
        for (Future<PaymentResponse> future : futures) {
            try {
                responses.add(future.get(30, TimeUnit.SECONDS));
            } catch (ExecutionException e) {
                // Some might fail, that's okay
            }
        }
        executor.shutdown();

        // All successful responses should be identical (idempotent)
        assertThat(responses).isNotEmpty();

        PaymentResponse first = responses.get(0);
        for (PaymentResponse response : responses) {
            assertThat(response.getCapturedAmount()).isEqualTo(first.getCapturedAmount());
            assertThat(response.getStatus()).isEqualTo(first.getStatus());
        }

        // Verify only one capture actually happened
        PaymentResponse finalState = paymentService.getPaymentWithTransactions(paymentId);
        assertThat(finalState.getCapturedAmount()).isEqualTo(2500L);
    }
}
