package com.paymentservice.controllers;

import com.paymentservice.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base Controller providing common response helper methods.
 *
 * All controllers should extend this class to ensure consistent
 * response handling across the application.
 *
 * Benefits:
 * - Eliminates response wrapping boilerplate
 * - Ensures consistent response structure
 * - Reduces code duplication
 * - Centralizes logging for response creation
 */
@Slf4j
public abstract class BaseController {

    /**
     * Creates a successful response with data only.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Creates a successful response with data and a message.
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /**
     * Creates a CREATED (201) response with data and a message.
     * Use this for POST operations that create new resources.
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, message));
    }

    /**
     * Creates a CREATED (201) response with data only.
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data));
    }

    /**
     * Creates a BAD_REQUEST (400) response with an error message.
     */
    protected <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * Creates a NOT_FOUND (404) response with an error message.
     */
    protected <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    /**
     * Creates a CONFLICT (409) response with an error message.
     * Use for idempotency conflicts or duplicate resource creation.
     */
    protected <T> ResponseEntity<ApiResponse<T>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    /**
     * Creates an UNPROCESSABLE_ENTITY (422) response with an error message.
     * Use for validation errors or business rule violations.
     */
    protected <T> ResponseEntity<ApiResponse<T>> unprocessable(String message) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(message));
    }

    /**
     * Creates a NO_CONTENT (204) response.
     * Use for DELETE operations or updates with no return data.
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates an ACCEPTED (202) response with data.
     * Use for async operations that have been accepted but not yet processed.
     */
    protected <T> ResponseEntity<ApiResponse<T>> accepted(T data, String message) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(data, message));
    }
}
