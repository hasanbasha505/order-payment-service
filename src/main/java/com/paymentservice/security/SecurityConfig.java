package com.paymentservice.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static com.paymentservice.security.SecurityConstants.*;

/**
 * Security Configuration - OAuth2 Resource Server with JWT validation.
 *
 * This configuration is ONLY active when security.enabled=true.
 * By default, security is DISABLED for development/demo purposes.
 *
 * Features:
 * - Stateless session management (JWT-based)
 * - OAuth2 Resource Server with JWT validation
 * - Scope-based authorization for all endpoints
 * - Method-level security via @PreAuthorize
 *
 * To enable security, set: security.enabled=true
 * And configure JWT issuer: spring.security.oauth2.resourceserver.jwt.issuer-uri
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = false)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())

            // Stateless session - no server-side session
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults()))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // ==================== Public Endpoints ====================
                .requestMatchers(
                    "/actuator/**",
                    "/actuator/health/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // ==================== Order Endpoints ====================
                .requestMatchers(HttpMethod.POST, "/api/v1/orders")
                    .hasAuthority(SCOPE_ORDERS_WRITE)
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/**")
                    .hasAuthority(SCOPE_ORDERS_READ)

                // ==================== Payment Endpoints ====================
                // Nested under orders following REST best practices
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/payment/authorize")
                    .hasAuthority(SCOPE_PAYMENTS_WRITE)
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/payment/capture")
                    .hasAuthority(SCOPE_PAYMENTS_WRITE)
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/payment/refund")
                    .hasAuthority(SCOPE_PAYMENTS_REFUND)
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/*/payment/**")
                    .hasAuthority(SCOPE_PAYMENTS_READ)

                // ==================== Reconciliation Endpoints ====================
                .requestMatchers(HttpMethod.POST, "/api/v1/reconciliation/**")
                    .hasAuthority(SCOPE_RECONCILIATION_WRITE)
                .requestMatchers(HttpMethod.GET, "/api/v1/reconciliation/**")
                    .hasAuthority(SCOPE_RECONCILIATION_READ)

                // ==================== Default ====================
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
