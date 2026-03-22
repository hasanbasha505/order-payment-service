package com.paymentservice.controllers;

import com.paymentservice.dto.request.CreateOrderRequest;
import com.paymentservice.dto.response.ApiResponse;
import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Order Controller - RESTful endpoints for order management.
 *
 * Follows REST best practices:
 * - Uses query parameters for filtering (restaurantId, customerId)
 * - Resource-based URLs without verbs
 * - Consistent response structure via ApiResponse wrapper
 *
 * Extends BaseController for common response helper methods.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController extends BaseController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with an associated payment in PENDING status")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(request, idempotencyKey);
        return created(response, "Order created successfully");
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Retrieves paginated list of orders with optional filtering by restaurant or customer")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @Parameter(description = "Filter by restaurant ID") @RequestParam(required = false) UUID restaurantId,
            @Parameter(description = "Filter by customer ID") @RequestParam(required = false) UUID customerId,
            Pageable pageable) {

        Page<OrderResponse> response;

        if (restaurantId != null) {
            response = orderService.getOrdersByRestaurant(restaurantId, pageable);
        } else if (customerId != null) {
            response = orderService.getOrdersByCustomer(customerId, pageable);
        } else {
            return badRequest("Either restaurantId or customerId query parameter is required");
        }

        return ok(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves order details including payment information")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {

        OrderResponse response = orderService.getOrder(orderId);
        return ok(response);
    }

    @GetMapping("/by-number/{orderNumber}")
    @Operation(summary = "Get order by order number", description = "Retrieves order by human-readable order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @Parameter(description = "Order number (e.g., ORD-2024-000001)") @PathVariable String orderNumber) {

        OrderResponse response = orderService.getOrderByOrderNumber(orderNumber);
        return ok(response);
    }
}
