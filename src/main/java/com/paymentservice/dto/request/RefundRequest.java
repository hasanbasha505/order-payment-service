package com.paymentservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    @NotBlank(message = "Refund reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
