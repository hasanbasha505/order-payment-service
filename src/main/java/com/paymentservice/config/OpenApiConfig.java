package com.paymentservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.paymentservice.security.SecurityConstants.*;

/**
 * OpenAPI/Swagger Configuration.
 *
 * Provides comprehensive API documentation including:
 * - API metadata (title, version, description, contact)
 * - OAuth2 security scheme with all available scopes
 * - Idempotency-Key header documentation
 * - Server information
 *
 * Access Swagger UI at: /swagger-ui.html
 * Access OpenAPI spec at: /api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Order Payment Service}")
    private String applicationName;

    @Value("${security.oauth2.token-url:https://auth.example.com/oauth/token}")
    private String tokenUrl;

    @Value("${security.oauth2.authorization-url:https://auth.example.com/oauth/authorize}")
    private String authorizationUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("/").description("Current Server"),
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.example.com").description("Production")
                ))
                .components(new Components()
                        // Idempotency Key header
                        .addSecuritySchemes("idempotency-key", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Idempotency-Key")
                                .description("Idempotency key for POST/PUT/PATCH requests (required)"))
                        // OAuth2 Security Scheme
                        .addSecuritySchemes("oauth2", oauth2SecurityScheme())
                        // Bearer Token (alternative)
                        .addSecuritySchemes("bearer", bearerSecurityScheme()))
                .addSecurityItem(new SecurityRequirement()
                        .addList("idempotency-key")
                        .addList("oauth2", List.of(
                                "orders:read", "orders:write",
                                "payments:read", "payments:write", "payments:refund",
                                "reconciliation:read", "reconciliation:write")));
    }

    private Info apiInfo() {
        return new Info()
                .title("Order Payment Lifecycle Service API")
                .description("""
                        Enterprise-grade payment lifecycle management service for a food delivery platform.

                        ## Features
                        - **Order Management**: Create and track orders
                        - **Payment Lifecycle**: Authorize, capture, and refund payments
                        - **Reconciliation**: Timezone-aware daily settlement reports
                        - **Idempotency**: Built-in request deduplication

                        ## REST Design
                        - Payments are child resources of Orders: `/orders/{orderId}/payment`
                        - Query parameters for filtering: `/orders?restaurantId=xxx`
                        - All amounts are in cents (smallest currency unit)

                        ## Security
                        This API supports OAuth2 authentication with the following scopes:
                        - `orders:read` / `orders:write` - Order operations
                        - `payments:read` / `payments:write` / `payments:refund` - Payment operations
                        - `reconciliation:read` / `reconciliation:write` - Reconciliation reports

                        **Note:** Security is disabled by default (security.enabled=false).
                        Set `security.enabled=true` in production.

                        ## Idempotency
                        All POST/PUT/PATCH requests require an `Idempotency-Key` header.
                        Keys are valid for 24 hours. Duplicate requests return cached responses.

                        ## Key Design Decisions
                        - Pessimistic locking for payment operations
                        - Transactional outbox pattern for event publishing
                        - Immutable transaction log for audit trail
                        - Circuit breaker for payment provider calls
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Payment Service Team")
                        .email("payment-team@example.com")
                        .url("https://github.com/example/order-payment-service"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme oauth2SecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("OAuth2 authentication with JWT tokens")
                .flows(new OAuthFlows()
                        // Client Credentials flow (service-to-service)
                        .clientCredentials(new OAuthFlow()
                                .tokenUrl(tokenUrl)
                                .scopes(oauth2Scopes()))
                        // Authorization Code flow (user-facing apps)
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl(authorizationUrl)
                                .tokenUrl(tokenUrl)
                                .scopes(oauth2Scopes())));
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer token authentication (alternative to OAuth2)");
    }

    private Scopes oauth2Scopes() {
        return new Scopes()
                // Order scopes
                .addString("orders:read", DESC_ORDERS_READ)
                .addString("orders:write", DESC_ORDERS_WRITE)
                // Payment scopes
                .addString("payments:read", DESC_PAYMENTS_READ)
                .addString("payments:write", DESC_PAYMENTS_WRITE)
                .addString("payments:refund", DESC_PAYMENTS_REFUND)
                // Reconciliation scopes
                .addString("reconciliation:read", DESC_RECONCILIATION_READ)
                .addString("reconciliation:write", DESC_RECONCILIATION_WRITE);
    }
}
