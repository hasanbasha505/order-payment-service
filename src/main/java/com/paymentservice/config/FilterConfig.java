package com.paymentservice.config;

import com.paymentservice.filters.IdempotencyFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration for servlet filters.
 */
@Configuration
public class FilterConfig {

    /**
     * Register the idempotency filter for payment API endpoints.
     */
    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(
            IdempotencyFilter idempotencyFilter) {

        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(idempotencyFilter);
        registration.addUrlPatterns("/api/v1/payments/*", "/api/v1/orders/*");
        registration.setName("idempotencyFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
