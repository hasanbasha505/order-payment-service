package com.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String errorCode;
    private String message;
    private String traceId;
    private Instant timestamp;
    private Map<String, Object> details;
    private String path;

    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }

    public static ErrorResponse of(String errorCode, String message, Map<String, Object> details) {
        return ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .details(details)
            .timestamp(Instant.now())
            .build();
    }
}
