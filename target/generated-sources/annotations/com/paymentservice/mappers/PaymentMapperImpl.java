package com.paymentservice.mappers;

import com.paymentservice.dto.response.PaymentResponse;
import com.paymentservice.models.Payment;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-22T23:06:06+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Ubuntu)"
)
@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Autowired
    private TransactionMapper transactionMapper;

    @Override
    public PaymentResponse toResponse(Payment payment) {
        if ( payment == null ) {
            return null;
        }

        PaymentResponse.PaymentResponseBuilder<?, ?> paymentResponse = PaymentResponse.builder();

        paymentResponse.paymentId( payment.getId() );
        paymentResponse.authorizationCode( payment.getProviderAuthCode() );
        paymentResponse.netAmount( calculateNetAmount( payment ) );
        paymentResponse.availableForRefund( calculateAvailableForRefund( payment ) );
        paymentResponse.createdAt( payment.getCreatedAt() );
        paymentResponse.updatedAt( payment.getUpdatedAt() );
        paymentResponse.orderId( payment.getOrderId() );
        paymentResponse.status( payment.getStatus() );
        paymentResponse.authorizedAmount( payment.getAuthorizedAmount() );
        paymentResponse.capturedAmount( payment.getCapturedAmount() );
        paymentResponse.refundedAmount( payment.getRefundedAmount() );
        paymentResponse.currencyCode( payment.getCurrencyCode() );
        paymentResponse.paymentMethod( payment.getPaymentMethod() );
        paymentResponse.paymentProvider( payment.getPaymentProvider() );
        paymentResponse.authorizedAt( payment.getAuthorizedAt() );
        paymentResponse.capturedAt( payment.getCapturedAt() );
        paymentResponse.expiresAt( payment.getExpiresAt() );
        paymentResponse.transactions( transactionMapper.toResponseList( payment.getTransactions() ) );

        return paymentResponse.build();
    }
}
