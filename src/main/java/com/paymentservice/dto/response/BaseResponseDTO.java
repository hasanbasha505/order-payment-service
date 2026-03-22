package com.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Base Response DTO providing common audit timestamp fields.
 *
 * All response DTOs should extend this class to ensure consistent
 * structure and reduce code duplication.
 *
 * Common fields:
 * - createdAt: When the resource was created
 * - updatedAt: When the resource was last modified
 *
 * Note: Uses @SuperBuilder for inheritance support with Lombok's builder pattern.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseResponseDTO {

    /**
     * Timestamp when the resource was created.
     */
    protected Instant createdAt;

    /**
     * Timestamp when the resource was last modified.
     */
    protected Instant updatedAt;
}
