package com.paymentservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Base Request DTO providing common fields for all request objects.
 *
 * All request DTOs that support metadata should extend this class
 * to ensure consistent validation and reduce code duplication.
 *
 * Common fields:
 * - metadata: Arbitrary key-value pairs for extensibility
 *
 * Note: Uses @SuperBuilder for inheritance support with Lombok's builder pattern.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseRequestDTO {

    /**
     * Optional metadata for custom key-value pairs.
     * Limited to 50 entries to prevent abuse.
     */
    @Size(max = 50, message = "Metadata cannot have more than 50 entries")
    protected Map<String, Object> metadata;
}
