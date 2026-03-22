package com.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.paymentservice.enums.MatchStatus;
import com.paymentservice.enums.ReconciliationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Reconciliation Report Response DTO.
 *
 * Extends BaseResponseDTO for common timestamp fields (createdAt, updatedAt).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReconciliationReportResponse extends BaseResponseDTO {

    private UUID reportId;
    private UUID restaurantId;
    private String restaurantName;
    private LocalDate reportDate;

    // Internal totals
    private Integer totalOrders;
    private Long totalCapturedAmount;
    private Long totalRefundedAmount;
    private Long netAmount;

    // Provider totals
    private Long providerTotalCaptured;
    private Long providerTotalRefunded;
    private Long providerNetAmount;

    // Variance
    private Long variance;

    private String currencyCode;
    private ReconciliationStatus status;
    private MatchStatus matchStatus;
    private Map<String, Object> discrepancies;

    private Instant generatedAt;
}
