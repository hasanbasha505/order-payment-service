package com.paymentservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Create Order Request DTO.
 *
 * Extends BaseRequestDTO for common metadata field.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateOrderRequest extends BaseRequestDTO {

    @NotNull(message = "Restaurant ID is required")
    private UUID restaurantId;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private Long totalAmount;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be uppercase ISO 4217 format")
    @lombok.Builder.Default
    private String currencyCode = "USD";
}
