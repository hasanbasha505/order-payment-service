package com.paymentservice.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResponse {

    private boolean success;
    private boolean timeout;
    private String transactionId;
    private String authorizationCode;
    private String errorCode;
    private String errorMessage;
    private Map<String, Object> rawResponse;

    public static ProviderResponse success(String transactionId, String authorizationCode) {
        return ProviderResponse.builder()
            .success(true)
            .timeout(false)
            .transactionId(transactionId)
            .authorizationCode(authorizationCode)
            .build();
    }

    public static ProviderResponse success(String transactionId) {
        return ProviderResponse.builder()
            .success(true)
            .timeout(false)
            .transactionId(transactionId)
            .build();
    }

    public static ProviderResponse failure(String errorCode, String errorMessage) {
        return ProviderResponse.builder()
            .success(false)
            .timeout(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build();
    }

    public static ProviderResponse timeout(String message) {
        return ProviderResponse.builder()
            .success(false)
            .timeout(true)
            .errorCode("TIMEOUT")
            .errorMessage(message)
            .build();
    }
}
