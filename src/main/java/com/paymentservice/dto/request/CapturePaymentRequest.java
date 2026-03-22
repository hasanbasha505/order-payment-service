package com.paymentservice.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapturePaymentRequest {

    /**
     * Amount to capture in smallest currency unit (cents).
     * If null, captures the full authorized amount.
     */
    @Positive(message = "Amount must be positive")
    private Long amount;
}
