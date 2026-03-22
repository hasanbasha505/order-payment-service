package com.paymentservice.mappers;

import com.paymentservice.dto.response.TransactionResponse;
import com.paymentservice.models.PaymentTransaction;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-22T23:06:06+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Ubuntu)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public TransactionResponse toResponse(PaymentTransaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        TransactionResponse.TransactionResponseBuilder<?, ?> transactionResponse = TransactionResponse.builder();

        transactionResponse.transactionId( transaction.getId() );
        transactionResponse.type( transaction.getTransactionType() );
        transactionResponse.providerTransactionId( transaction.getProviderTxnId() );
        transactionResponse.createdAt( transaction.getCreatedAt() );
        transactionResponse.amount( transaction.getAmount() );
        transactionResponse.currencyCode( transaction.getCurrencyCode() );
        transactionResponse.status( transaction.getStatus() );
        transactionResponse.errorCode( transaction.getErrorCode() );
        transactionResponse.errorMessage( transaction.getErrorMessage() );

        return transactionResponse.build();
    }

    @Override
    public List<TransactionResponse> toResponseList(List<PaymentTransaction> transactions) {
        if ( transactions == null ) {
            return null;
        }

        List<TransactionResponse> list = new ArrayList<TransactionResponse>( transactions.size() );
        for ( PaymentTransaction paymentTransaction : transactions ) {
            list.add( toResponse( paymentTransaction ) );
        }

        return list;
    }
}
