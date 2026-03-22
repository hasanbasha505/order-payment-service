package com.paymentservice.integration;

import com.paymentservice.dto.request.CreateOrderRequest;
import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.enums.OrderStatus;
import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Order Integration Tests")
class OrderIntegrationTest {

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

    private UUID testRestaurantId;

    @BeforeEach
    void setUp() {
        testRestaurantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    }

    @Test
    @DisplayName("should create order with payment")
    void shouldCreateOrderWithPayment() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("items", "burger, fries");
        metadata.put("deliveryAddress", "123 Main St");

        CreateOrderRequest request = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(2500L)
                .currencyCode("USD")
                .metadata(metadata)
                .build();

        OrderResponse response = orderService.createOrder(request, "order-" + UUID.randomUUID());

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getOrderNumber()).isNotNull();
        assertThat(response.getOrderNumber()).startsWith("ORD-");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getTotalAmount()).isEqualTo(2500L);
        assertThat(response.getCurrencyCode()).isEqualTo("USD");

        // Check payment was created
        assertThat(response.getPayment()).isNotNull();
        assertThat(response.getPayment().getPaymentId()).isNotNull();
        assertThat(response.getPayment().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("should retrieve order by ID")
    void shouldRetrieveOrderById() {
        // Create order first
        CreateOrderRequest request = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(3000L)
                .currencyCode("USD")
                .build();

        OrderResponse created = orderService.createOrder(request, "order-" + UUID.randomUUID());

        // Retrieve it
        OrderResponse retrieved = orderService.getOrder(created.getOrderId());

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getOrderId()).isEqualTo(created.getOrderId());
        assertThat(retrieved.getOrderNumber()).isEqualTo(created.getOrderNumber());
        assertThat(retrieved.getTotalAmount()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("should list orders with pagination")
    void shouldListOrdersWithPagination() {
        // Create multiple orders
        for (int i = 0; i < 5; i++) {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .restaurantId(testRestaurantId)
                    .customerId(UUID.randomUUID())
                    .totalAmount(1000L + (i * 100))
                    .currencyCode("USD")
                    .build();
            orderService.createOrder(request, "order-" + UUID.randomUUID());
        }

        // List with pagination
        Page<OrderResponse> page = orderService.getOrdersByRestaurant(testRestaurantId, PageRequest.of(0, 3));

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().size()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("should generate unique order numbers")
    void shouldGenerateUniqueOrderNumbers() {
        CreateOrderRequest request1 = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(1000L)
                .currencyCode("USD")
                .build();

        CreateOrderRequest request2 = CreateOrderRequest.builder()
                .restaurantId(testRestaurantId)
                .customerId(UUID.randomUUID())
                .totalAmount(2000L)
                .currencyCode("USD")
                .build();

        OrderResponse order1 = orderService.createOrder(request1, "order-" + UUID.randomUUID());
        OrderResponse order2 = orderService.createOrder(request2, "order-" + UUID.randomUUID());

        assertThat(order1.getOrderNumber()).isNotEqualTo(order2.getOrderNumber());
    }
}
