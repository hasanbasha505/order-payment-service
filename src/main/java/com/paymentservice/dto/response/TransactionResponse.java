package com.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.paymentservice.enums.TransactionStatus;
import com.paymentservice.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Transaction Response DTO.
 *
 * Extends BaseResponseDTO for common timestamp fields.
 * Note: Transactions are immutable, so only createdAt is populated (updatedAt will be null).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse extends BaseResponseDTO {

    private UUID transactionId;
    private TransactionType type;
    private Long amount;
    private String currencyCode;
    private TransactionStatus status;
    private String providerTransactionId;
    private String errorCode;
    private String errorMessage;
}
