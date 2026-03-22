package com.paymentservice.security;

/**
 * Fine-grained Permissions - Used for method-level security and audit logging.
 *
 * These permissions provide granular access control beyond OAuth2 scopes.
 * They can be used with custom permission evaluators for complex authorization logic.
 *
 * Permission naming convention: resource:action[:qualifier]
 * Examples:
 * - order:create
 * - payment:authorize
 * - reconciliation:generate
 */
public final class Permissions {

    private Permissions() {
        // Utility class - prevent instantiation
    }

    // ==================== Order Permissions ====================

    /** Permission to create new orders */
    public static final String ORDER_CREATE = "order:create";

    /** Permission to read own orders */
    public static final String ORDER_READ = "order:read";

    /** Permission to read all orders (admin/restaurant) */
    public static final String ORDER_READ_ALL = "order:read:all";

    /** Permission to cancel orders */
    public static final String ORDER_CANCEL = "order:cancel";

    // ==================== Payment Permissions ====================

    /** Permission to authorize payments */
    public static final String PAYMENT_AUTHORIZE = "payment:authorize";

    /** Permission to capture authorized payments */
    public static final String PAYMENT_CAPTURE = "payment:capture";

    /** Permission to refund payments */
    public static final String PAYMENT_REFUND = "payment:refund";

    /** Permission to view payment details */
    public static final String PAYMENT_READ = "payment:read";

    /** Permission to void/cancel payments */
    public static final String PAYMENT_VOID = "payment:void";

    // ==================== Reconciliation Permissions ====================

    /** Permission to generate reconciliation reports */
    public static final String RECONCILIATION_GENERATE = "reconciliation:generate";

    /** Permission to view reconciliation reports */
    public static final String RECONCILIATION_READ = "reconciliation:read";

    /** Permission to mark reports as investigated */
    public static final String RECONCILIATION_INVESTIGATE = "reconciliation:investigate";

    /** Permission to resolve discrepancies */
    public static final String RECONCILIATION_RESOLVE = "reconciliation:resolve";

    // ==================== Admin Permissions ====================

    /** Full admin access - bypasses all permission checks */
    public static final String ADMIN_FULL_ACCESS = "admin:full";

    /** Permission to view audit logs */
    public static final String ADMIN_AUDIT_READ = "admin:audit:read";

    /** Permission to manage system configuration */
    public static final String ADMIN_CONFIG = "admin:config";
}
