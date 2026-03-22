package com.paymentservice.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Disabled Security Configuration - Permits all requests without authentication.
 *
 * This is the DEFAULT configuration (security.enabled=false or not set).
 * Used for development, testing, and demo/interview purposes.
 *
 * WARNING: Do NOT use in production! Set security.enabled=true for production.
 *
 * Features:
 * - All requests are permitted without authentication
 * - CSRF disabled for API compatibility
 * - No session management required
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "security.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledSecurityConfig {

    @Bean
    public SecurityFilterChain disabledSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API
            .csrf(csrf -> csrf.disable())

            // Permit all requests - NO authentication required
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
