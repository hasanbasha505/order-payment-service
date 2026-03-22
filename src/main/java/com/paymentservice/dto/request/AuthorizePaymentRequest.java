package com.paymentservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Authorize Payment Request DTO.
 *
 * Extends BaseRequestDTO for common metadata field.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthorizePaymentRequest extends BaseRequestDTO {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @Size(max = 500, message = "Payment token cannot exceed 500 characters")
    private String paymentToken;
}
