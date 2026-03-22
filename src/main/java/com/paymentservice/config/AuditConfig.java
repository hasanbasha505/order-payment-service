package com.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing Configuration.
 *
 * Enables automatic population of audit fields:
 * - @CreatedBy: Populated on entity creation
 * - @LastModifiedBy: Populated on every update
 *
 * The auditor is determined from the Spring Security context.
 * When security is disabled, defaults to "system".
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    /**
     * Default auditor name when no security context is available.
     */
    private static final String SYSTEM_USER = "system";

    /**
     * Provides the current auditor for JPA auditing.
     *
     * Resolution order:
     * 1. Authenticated user from Security Context
     * 2. "system" as fallback (for background jobs, disabled security)
     *
     * @return AuditorAware implementation that resolves the current user
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(SYSTEM_USER);
            }

            String principal = authentication.getName();

            // Handle anonymous users
            if ("anonymousUser".equals(principal)) {
                return Optional.of(SYSTEM_USER);
            }

            return Optional.of(principal);
        };
    }
}
