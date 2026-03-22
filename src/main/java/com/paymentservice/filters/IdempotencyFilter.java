package com.paymentservice.filters;

import com.paymentservice.models.IdempotencyKey;
import com.paymentservice.repositories.IdempotencyKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * HTTP-level idempotency filter.
 * For POST, PUT, PATCH requests, requires an Idempotency-Key header.
 * If the same key was used before with the same request body, returns cached response.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final Set<String> IDEMPOTENT_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final Set<String> IDEMPOTENT_PATHS = Set.of(
            "/api/v1/payments/",
            "/api/v1/orders"
    );

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Value("${payment.idempotency.key-ttl-hours:24}")
    private int keyTtlHours;

    @Value("${payment.idempotency.max-body-size-bytes:1048576}")
    private int maxBodySizeBytes; // Default 1MB

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only process idempotent methods for specific paths
        if (!requiresIdempotency(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        // For payment operations, idempotency key is required
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // Let the controller handle missing key validation
            filterChain.doFilter(request, response);
            return;
        }

        // Check content length before reading
        int contentLength = request.getContentLength();
        if (contentLength > maxBodySizeBytes) {
            log.warn("Request body too large: {} bytes (max: {})", contentLength, maxBodySizeBytes);
            sendError(response, request.getRequestURI(), 413,
                    "PAYLOAD_TOO_LARGE",
                    "Request body exceeds maximum allowed size of " + maxBodySizeBytes + " bytes");
            return;
        }

        // Wrap request to read body multiple times (reads body upfront)
        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request, maxBodySizeBytes);
        String requestBody = wrappedRequest.getCachedBody();
        String currentHash = computeRequestHash(request.getMethod(), request.getRequestURI(), requestBody);

        // Check for existing idempotency key BEFORE calling filter chain
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKey(idempotencyKey);

        if (existingKey.isPresent()) {
            IdempotencyKey existing = existingKey.get();

            // Check if expired
            if (existing.isExpired()) {
                log.debug("Idempotency key expired, processing as new request: {}", idempotencyKey);
                idempotencyKeyRepository.delete(existing);
                // Fall through to process as new request
            } else {
                // Validate request hash matches
                if (!existing.getRequestHash().equals(currentHash)) {
                    log.warn("Idempotency key reused with different request body: {}", idempotencyKey);
                    sendError(response, request.getRequestURI(), 422,
                            "IDEMPOTENCY_KEY_MISMATCH",
                            "Idempotency key was used with a different request body");
                    return;
                }

                // Return cached response directly WITHOUT calling filter chain
                if (existing.getResponseCode() != null && existing.getResponseBody() != null) {
                    log.info("Returning cached idempotent response for key: {}", idempotencyKey);
                    response.setStatus(existing.getResponseCode());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(existing.getResponseBody());
                    return;
                }

                // No cached response, fall through to reprocess
                log.debug("No cached response for key, reprocessing: {}", idempotencyKey);
            }
        }

        // Process the request (new request or expired/missing cache)
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        boolean requestProcessed = false;
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            requestProcessed = true;

            // Store idempotency key with response - MUST succeed for idempotency guarantee
            String responseBody = getResponseBody(wrappedResponse);

            IdempotencyKey newKey = IdempotencyKey.builder()
                    .key(idempotencyKey)
                    .requestHash(currentHash)
                    .responseCode(wrappedResponse.getStatus())
                    .responseBody(responseBody)
                    .expiresAt(Instant.now().plus(Duration.ofHours(keyTtlHours)))
                    .build();

            idempotencyKeyRepository.save(newKey);
            log.debug("Stored idempotency key: {}", idempotencyKey);

            wrappedResponse.copyBodyToResponse();
        } catch (Exception e) {
            if (requestProcessed) {
                // Request was processed but idempotency key storage failed
                // This is critical - client may retry and cause duplicate processing
                log.error("CRITICAL: Failed to store idempotency key after processing request: {}", idempotencyKey, e);
                // Still return the response but log critical error for monitoring/alerting
                wrappedResponse.copyBodyToResponse();
            } else {
                throw e; // Request processing failed, propagate exception
            }
        }
    }

    private boolean requiresIdempotency(HttpServletRequest request) {
        if (!IDEMPOTENT_METHODS.contains(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        return IDEMPOTENT_PATHS.stream().anyMatch(path::startsWith);
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String computeRequestHash(String method, String uri, String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = method + ":" + uri + ":" + body;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void sendError(HttpServletResponse response, String path, int status,
                           String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String timestamp = Instant.now().toString();

        String errorJson = String.format(
                "{\"errorCode\":\"%s\",\"message\":\"%s\",\"traceId\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                errorCode, message, traceId, path, timestamp);

        response.getWriter().write(errorJson);
    }

    /**
     * Request wrapper that reads and caches body upfront for multiple reads.
     * Enforces maximum body size to prevent OOM attacks.
     */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyRequestWrapper(HttpServletRequest request, int maxBodySizeBytes) throws IOException {
            super(request);
            // Read with size limit to prevent OOM
            byte[] body = request.getInputStream().readNBytes(maxBodySizeBytes + 1);
            if (body.length > maxBodySizeBytes) {
                throw new IOException("Request body exceeds maximum allowed size");
            }
            this.cachedBody = body;
        }

        public String getCachedBody() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    /**
     * ServletInputStream backed by cached byte array.
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException("Async not supported");
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
