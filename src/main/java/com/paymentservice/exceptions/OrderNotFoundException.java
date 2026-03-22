package com.paymentservice.exceptions;

import java.util.UUID;

public class OrderNotFoundException extends PaymentException {

    public OrderNotFoundException(UUID orderId) {
        super("ORDER_NOT_FOUND", "Order not found: " + orderId);
    }

    public OrderNotFoundException(String orderNumber) {
        super("ORDER_NOT_FOUND", "Order not found: " + orderNumber);
    }
}
