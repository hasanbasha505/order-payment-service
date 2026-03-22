package com.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.paymentservice.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payment Response DTO.
 *
 * Extends BaseResponseDTO for common audit timestamp fields (createdAt, updatedAt).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse extends BaseResponseDTO {

    private UUID paymentId;
    private UUID orderId;
    private PaymentStatus status;
    private Long authorizedAmount;
    private Long capturedAmount;
    private Long refundedAmount;
    private Long netAmount;
    private Long availableForRefund;
    private String currencyCode;
    private String paymentMethod;
    private String paymentProvider;
    private String authorizationCode;
    private Instant authorizedAt;
    private Instant capturedAt;
    private Instant expiresAt;
    private List<TransactionResponse> transactions;
}
