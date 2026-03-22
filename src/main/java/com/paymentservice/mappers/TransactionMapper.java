package com.paymentservice.mappers;

import com.paymentservice.dto.response.TransactionResponse;
import com.paymentservice.models.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for PaymentTransaction entity to TransactionResponse DTO.
 *
 * Maps:
 * - id -> transactionId
 * - transactionType -> type
 * - providerTxnId -> providerTransactionId
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    @Mapping(source = "id", target = "transactionId")
    @Mapping(source = "transactionType", target = "type")
    @Mapping(source = "providerTxnId", target = "providerTransactionId")
    TransactionResponse toResponse(PaymentTransaction transaction);

    List<TransactionResponse> toResponseList(List<PaymentTransaction> transactions);
}
