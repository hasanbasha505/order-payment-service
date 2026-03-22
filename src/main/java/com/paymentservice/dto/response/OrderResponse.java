package com.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.paymentservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * Order Response DTO.
 *
 * Extends BaseResponseDTO for common audit timestamp fields (createdAt, updatedAt).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse extends BaseResponseDTO {

    private UUID orderId;
    private String orderNumber;
    private UUID restaurantId;
    private String restaurantName;
    private UUID customerId;
    private OrderStatus status;
    private Long totalAmount;
    private String currencyCode;
    private Map<String, Object> metadata;
    private PaymentResponse payment;
}
