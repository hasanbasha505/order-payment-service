package com.paymentservice.concurrency;

import com.paymentservice.dto.request.AuthorizePaymentRequest;
import com.paymentservice.dto.request.CapturePaymentRequest;
import com.paymentservice.dto.request.CreateOrderRequest;
import com.paymentservice.dto.request.RefundRequest;
import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.exceptions.InsufficientRefundBalanceException;
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
@DisplayName("Concurrent Refund Tests")
class ConcurrentRefundTest {

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
    @DisplayName("should prevent over-refund under concurrent requests")
    void shouldPreventOverRefundUnderConcurrentRequests() throws Exception {
        // Create, authorize, and capture order for $25
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

        paymentService.capturePayment(paymentId,
                CapturePaymentRequest.builder().amount(2500L).build(),
                "capture-" + UUID.randomUUID());

        // Attempt 10 concurrent refunds of $25 each (only 1 should succeed for full refund)
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientBalanceCount = new AtomicInteger(0);
        AtomicInteger otherFailureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    RefundRequest refundRequest = RefundRequest.builder()
                            .amount(2500L)  // Full refund attempt
                            .reason("Concurrent refund test " + threadNum)
                            .build();

                    paymentService.refundPayment(paymentId, refundRequest, "refund-" + threadNum + "-" + UUID.randomUUID());
                    successCount.incrementAndGet();
                } catch (InsufficientRefundBalanceException e) {
                    insufficientBalanceCount.incrementAndGet();
                } catch (Exception e) {
                    otherFailureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Only ONE full refund should succeed
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(insufficientBalanceCount.get()).isEqualTo(numThreads - 1);
        assertThat(otherFailureCount.get()).isEqualTo(0);

        // Verify final state
        PaymentResponse finalState = paymentService.getPayment(paymentId);
        assertThat(finalState.getStatus()).isEqualTo(PaymentStatus.FULLY_REFUNDED);
        assertThat(finalState.getRefundedAmount()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("should correctly handle concurrent partial refunds")
    void shouldCorrectlyHandleConcurrentPartialRefunds() throws Exception {
        // Create, authorize, and capture order for $100
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(10000L)  // $100
                .currencyCode("USD")
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest, "order-" + UUID.randomUUID());
        UUID paymentId = orderResponse.getPayment().getPaymentId();

        paymentService.authorizePayment(paymentId,
                AuthorizePaymentRequest.builder()
                        .amount(10000L)
                        .paymentMethod("CARD")
                        .paymentToken("tok_valid")
                        .build(),
                "auth-" + UUID.randomUUID());

        paymentService.capturePayment(paymentId,
                CapturePaymentRequest.builder().amount(10000L).build(),
                "capture-" + UUID.randomUUID());

        // Attempt 10 concurrent refunds of $15 each (total $150, only $100 available)
        int numThreads = 10;
        long refundAmount = 1500L;  // $15 per refund

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Long> successfulRefundAmounts = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    RefundRequest refundRequest = RefundRequest.builder()
                            .amount(refundAmount)
                            .reason("Partial refund " + threadNum)
                            .build();

                    PaymentResponse response = paymentService.refundPayment(
                            paymentId, refundRequest, "refund-" + threadNum + "-" + UUID.randomUUID());
                    successCount.incrementAndGet();
                    successfulRefundAmounts.add(refundAmount);
                } catch (InsufficientRefundBalanceException e) {
                    // Expected for some requests
                } catch (Exception e) {
                    // Other exceptions
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify total refunded amount doesn't exceed captured
        PaymentResponse finalState = paymentService.getPayment(paymentId);
        assertThat(finalState.getRefundedAmount()).isLessThanOrEqualTo(10000L);

        // Should have 6 successful refunds (6 * $15 = $90, leaving $10)
        // The 7th would exceed (7 * $15 = $105 > $100)
        assertThat(successCount.get()).isLessThanOrEqualTo(6);

        // Verify balance is consistent
        long expectedRemaining = 10000L - finalState.getRefundedAmount();
        assertThat(expectedRemaining).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("should maintain data consistency under high contention")
    void shouldMaintainDataConsistencyUnderHighContention() throws Exception {
        // Create and capture payment
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(5000L)  // $50
                .currencyCode("USD")
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest, "order-" + UUID.randomUUID());
        UUID paymentId = orderResponse.getPayment().getPaymentId();

        paymentService.authorizePayment(paymentId,
                AuthorizePaymentRequest.builder()
                        .amount(5000L)
                        .paymentMethod("CARD")
                        .paymentToken("tok_valid")
                        .build(),
                "auth-" + UUID.randomUUID());

        paymentService.capturePayment(paymentId,
                CapturePaymentRequest.builder().amount(5000L).build(),
                "capture-" + UUID.randomUUID());

        // High contention: 50 threads, each trying to refund $5
        int numThreads = 50;
        long refundAmount = 500L;  // $5

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                try {
                    paymentService.refundPayment(paymentId,
                            RefundRequest.builder()
                                    .amount(refundAmount)
                                    .reason("High contention test " + threadNum)
                                    .build(),
                            "refund-" + threadNum + "-" + UUID.randomUUID());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        startLatch.countDown();

        int successes = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(30, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        executor.shutdown();

        // Verify consistency
        PaymentResponse finalState = paymentService.getPayment(paymentId);

        // Total refunded should equal successes * refundAmount
        assertThat(finalState.getRefundedAmount()).isEqualTo(successes * refundAmount);

        // Refunded amount should not exceed captured
        assertThat(finalState.getRefundedAmount()).isLessThanOrEqualTo(5000L);

        // Should have exactly 10 successful refunds (10 * $5 = $50)
        assertThat(successes).isEqualTo(10);
    }
}
