package com.paymentservice.security;

/**
 * Security Constants - Centralized definition of OAuth2 scopes and roles.
 *
 * OAuth2 Scopes follow the pattern: resource:action
 * - orders:read, orders:write
 * - payments:read, payments:write, payments:refund
 * - reconciliation:read, reconciliation:write
 *
 * Note: Spring Security prepends "SCOPE_" to JWT scope claims automatically.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class - prevent instantiation
    }

    // ==================== OAuth2 Scopes ====================
    // These are used with @PreAuthorize("hasAuthority('SCOPE_xxx')")

    // Order Scopes
    public static final String SCOPE_ORDERS_READ = "SCOPE_orders:read";
    public static final String SCOPE_ORDERS_WRITE = "SCOPE_orders:write";

    // Payment Scopes
    public static final String SCOPE_PAYMENTS_READ = "SCOPE_payments:read";
    public static final String SCOPE_PAYMENTS_WRITE = "SCOPE_payments:write";
    public static final String SCOPE_PAYMENTS_REFUND = "SCOPE_payments:refund";

    // Reconciliation Scopes
    public static final String SCOPE_RECONCILIATION_READ = "SCOPE_reconciliation:read";
    public static final String SCOPE_RECONCILIATION_WRITE = "SCOPE_reconciliation:write";

    // ==================== Roles ====================
    // Role-based access control for coarse-grained permissions

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_RESTAURANT = "ROLE_RESTAURANT";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_SYSTEM = "ROLE_SYSTEM";
    public static final String ROLE_FINANCE = "ROLE_FINANCE";

    // ==================== Scope Descriptions (for OpenAPI documentation) ====================

    public static final String DESC_ORDERS_READ = "Read order information";
    public static final String DESC_ORDERS_WRITE = "Create and modify orders";
    public static final String DESC_PAYMENTS_READ = "Read payment information";
    public static final String DESC_PAYMENTS_WRITE = "Authorize and capture payments";
    public static final String DESC_PAYMENTS_REFUND = "Process payment refunds";
    public static final String DESC_RECONCILIATION_READ = "View reconciliation reports";
    public static final String DESC_RECONCILIATION_WRITE = "Generate reconciliation reports";
}
