package com.paymentservice.mappers;

import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.models.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Payment entity to PaymentResponse DTO.
 *
 * Maps:
 * - id -> paymentId
 * - providerAuthCode -> authorizationCode
 * - Calculates netAmount and availableForRefund from entity methods
 * - Uses TransactionMapper for nested transactions
 */
@Mapper(componentModel = "spring",
        uses = {TransactionMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "providerAuthCode", target = "authorizationCode")
    @Mapping(source = "payment", target = "netAmount", qualifiedByName = "calculateNetAmount")
    @Mapping(source = "payment", target = "availableForRefund", qualifiedByName = "calculateAvailableForRefund")
    PaymentResponse toResponse(Payment payment);

    @Named("calculateNetAmount")
    default Long calculateNetAmount(Payment payment) {
        if (payment == null) {
            return null;
        }
        return payment.getNetAmount();
    }

    @Named("calculateAvailableForRefund")
    default Long calculateAvailableForRefund(Payment payment) {
        if (payment == null) {
            return null;
        }
        return payment.getAvailableForRefund();
    }
}
