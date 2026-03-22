package com.paymentservice.services;

import com.paymentservice.dto.request.CreateOrderRequest;
import com.paymentservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey);

    OrderResponse getOrder(UUID orderId);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    Page<OrderResponse> getOrdersByRestaurant(UUID restaurantId, Pageable pageable);

    Page<OrderResponse> getOrdersByCustomer(UUID customerId, Pageable pageable);
}
