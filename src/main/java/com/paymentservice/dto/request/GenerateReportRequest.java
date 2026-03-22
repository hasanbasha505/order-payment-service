package com.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReportRequest {

    @NotNull(message = "Restaurant ID is required")
    private UUID restaurantId;

    @NotNull(message = "Report date is required")
    @PastOrPresent(message = "Report date cannot be in the future")
    private LocalDate reportDate;

    /**
     * If true, compares with payment provider data.
     */
    @Builder.Default
    private boolean compareWithProvider = false;
}
