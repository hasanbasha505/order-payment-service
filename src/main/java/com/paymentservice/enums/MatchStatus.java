package com.paymentservice.enums;

/**
 * Match status for reconciliation comparison with payment provider.
 */
public enum MatchStatus {
    PENDING,       // Comparison not yet performed
    MATCHED,       // Internal data matches provider data
    MISMATCH,      // Discrepancies found
    INVESTIGATING, // Under investigation
    RESOLVED       // Discrepancies resolved
}
