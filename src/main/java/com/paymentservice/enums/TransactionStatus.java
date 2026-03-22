package com.paymentservice.enums;

/**
 * Status of a payment transaction.
 */
public enum TransactionStatus {
    INITIATED,  // Transaction created, provider call pending
    SUCCESS,    // Provider confirmed success
    FAILED,     // Provider confirmed failure
    TIMEOUT     // Provider did not respond in time
}
