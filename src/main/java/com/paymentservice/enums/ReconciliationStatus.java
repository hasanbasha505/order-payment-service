package com.paymentservice.enums;

/**
 * Status of a reconciliation report.
 */
public enum ReconciliationStatus {
    PENDING,     // Report not yet generated
    GENERATED,   // Report generated, awaiting verification
    VERIFIED,    // Report verified and approved
    DISPUTED     // Discrepancies found, under investigation
}
