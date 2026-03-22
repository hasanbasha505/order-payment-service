package com.paymentservice.services.impl;

import com.paymentservice.dto.request.CreateOrderRequest;
import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.enums.OrderStatus;
import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.exceptions.OrderNotFoundException;
import com.paymentservice.models.Order;
import com.paymentservice.models.Payment;
import com.paymentservice.models.Restaurant;
import com.paymentservice.repositories.OrderRepository;
import com.paymentservice.repositories.PaymentRepository;
import com.paymentservice.repositories.RestaurantRepository;
import com.paymentservice.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RestaurantRepository restaurantRepository;

    private final AtomicLong orderSequence = new AtomicLong(System.currentTimeMillis() % 1000000);

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        log.info("Creating order: restaurantId={}, customerId={}, amount={}",
            request.getRestaurantId(), request.getCustomerId(), request.getTotalAmount());

        // Validate restaurant exists
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
            .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + request.getRestaurantId()));

        // Generate unique order number
        String orderNumber = generateOrderNumber();

        // Create order
        Order order = Order.builder()
            .orderNumber(orderNumber)
            .restaurantId(request.getRestaurantId())
            .customerId(request.getCustomerId())
            .status(OrderStatus.CREATED)
            .totalAmount(request.getTotalAmount())
            .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : restaurant.getCurrencyCode())
            .metadata(request.getMetadata())
            .build();

        order = orderRepository.save(order);

        // Create associated payment in PENDING state
        Payment payment = Payment.builder()
            .orderId(order.getId())
            .status(PaymentStatus.PENDING)
            .currencyCode(order.getCurrencyCode())
            .build();

        payment = paymentRepository.save(payment);

        log.info("Order created: orderId={}, orderNumber={}, paymentId={}",
            order.getId(), orderNumber, payment.getId());

        return toOrderResponse(order, payment, restaurant.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findByIdWithPayment(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        Restaurant restaurant = restaurantRepository.findById(order.getRestaurantId())
            .orElse(null);

        return toOrderResponse(order, order.getPayment(),
            restaurant != null ? restaurant.getName() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new OrderNotFoundException(orderNumber));

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        Restaurant restaurant = restaurantRepository.findById(order.getRestaurantId())
            .orElse(null);

        return toOrderResponse(order, payment,
            restaurant != null ? restaurant.getName() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByRestaurant(UUID restaurantId, Pageable pageable) {
        // Use JOIN FETCH to avoid N+1 queries
        return orderRepository.findByRestaurantIdWithPayment(restaurantId, pageable)
            .map(order -> toOrderResponse(order, order.getPayment(), null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        // Use JOIN FETCH to avoid N+1 queries
        return orderRepository.findByCustomerIdWithPayment(customerId, pageable)
            .map(order -> toOrderResponse(order, order.getPayment(), null));
    }

    private String generateOrderNumber() {
        int year = Year.now().getValue();
        long sequence = orderSequence.incrementAndGet();
        return String.format("ORD-%d-%06d", year, sequence % 1000000);
    }

    private OrderResponse toOrderResponse(Order order, Payment payment, String restaurantName) {
        PaymentResponse paymentResponse = null;
        if (payment != null) {
            paymentResponse = PaymentResponse.builder()
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
                .updatedAt(payment.getUpdatedAt())
                .build();
        }

        return OrderResponse.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderNumber())
            .restaurantId(order.getRestaurantId())
            .restaurantName(restaurantName)
            .customerId(order.getCustomerId())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .currencyCode(order.getCurrencyCode())
            .metadata(order.getMetadata())
            .payment(paymentResponse)
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }
}
