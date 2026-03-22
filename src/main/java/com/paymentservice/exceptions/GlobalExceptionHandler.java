package com.paymentservice.exceptions;

import com.paymentservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException ex, HttpServletRequest request) {
        log.warn("Payment not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException ex, HttpServletRequest request) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentState(
            InvalidPaymentStateException ex, HttpServletRequest request) {
        log.warn("Invalid payment state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(DuplicateCaptureException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCapture(
            DuplicateCaptureException ex, HttpServletRequest request) {
        log.warn("Duplicate capture attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(InsufficientRefundBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientRefundBalance(
            InsufficientRefundBalanceException ex, HttpServletRequest request) {
        log.warn("Insufficient refund balance: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(AuthorizationExpiredException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationExpired(
            AuthorizationExpiredException ex, HttpServletRequest request) {
        log.warn("Authorization expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
            IdempotencyConflictException ex, HttpServletRequest request) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Missing required header: {}", ex.getHeaderName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode("MISSING_HEADER")
                .message("Required header missing: " + ex.getHeaderName())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .details(errors)
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic locking failure - concurrent modification detected");
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.builder()
                .errorCode("CONCURRENT_MODIFICATION")
                .message("Resource was modified by another request. Please retry.")
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.builder()
                .errorCode("DATA_INTEGRITY_ERROR")
                .message("Data integrity constraint violated")
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex, HttpServletRequest request) {
        log.error("Payment error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .errorCode("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build());
    }

    private String getTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
