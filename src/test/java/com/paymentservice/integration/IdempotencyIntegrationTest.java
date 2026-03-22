package com.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentservice.dto.request.CreateOrderRequest;
import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Idempotency Integration Tests")
class IdempotencyIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    private UUID testRestaurantId;

    @BeforeEach
    void setUp() {
        testRestaurantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    }

    @Test
    @DisplayName("should return same response for duplicate authorize requests")
    void shouldReturnSameResponseForDuplicateAuthorizeRequests() throws Exception {
        // Create order first
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(2500L)
                .currencyCode("USD")
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest, "order-" + UUID.randomUUID());
        UUID orderId = orderResponse.getOrderId();

        String idempotencyKey = "auth-" + UUID.randomUUID();
        String authRequestJson = """
                {
                    "amount": 2500,
                    "paymentMethod": "CARD",
                    "paymentToken": "tok_valid"
                }
                """;

        // First request - using new RESTful URL: /orders/{orderId}/payment/authorize
        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/authorize", orderId)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AUTHORIZED"));

        // Second request with same key - should return cached response
        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/authorize", orderId)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AUTHORIZED"));
    }

    @Test
    @DisplayName("should reject idempotency key reuse with different body")
    void shouldRejectIdempotencyKeyReuseWithDifferentBody() throws Exception {
        // Create order
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(2500L)
                .currencyCode("USD")
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest, "order-" + UUID.randomUUID());
        UUID orderId = orderResponse.getOrderId();

        String idempotencyKey = "auth-" + UUID.randomUUID();

        // First request
        String firstRequest = """
                {
                    "amount": 2500,
                    "paymentMethod": "CARD",
                    "paymentToken": "tok_valid"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/authorize", orderId)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isOk());

        // Second request with same key but different body
        String differentRequest = """
                {
                    "amount": 3000,
                    "paymentMethod": "CARD",
                    "paymentToken": "tok_valid"
                }
                """;

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/authorize", orderId)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(differentRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("should require idempotency key for payment operations")
    void shouldRequireIdempotencyKeyForPaymentOperations() throws Exception {
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(2500L)
                .currencyCode("USD")
                .build();

        OrderResponse orderResponse = orderService.createOrder(orderRequest, "order-" + UUID.randomUUID());
        UUID orderId = orderResponse.getOrderId();

        String authRequestJson = """
                {
                    "amount": 2500,
                    "paymentMethod": "CARD",
                    "paymentToken": "tok_valid"
                }
                """;

        // Request without Idempotency-Key header
        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/authorize", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authRequestJson))
                .andExpect(status().isBadRequest());
    }
}
