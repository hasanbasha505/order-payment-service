package com.paymentservice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for monetary calculations.
 * All internal amounts are stored in cents (smallest currency unit).
 */
public final class MoneyUtil {

    private static final int CENTS_PER_DOLLAR = 100;
    private static final int SCALE = 2;

    private MoneyUtil() {
        // Utility class
    }

    /**
     * Convert dollars to cents.
     */
    public static long toCents(BigDecimal dollars) {
        if (dollars == null) {
            return 0L;
        }
        return dollars.multiply(BigDecimal.valueOf(CENTS_PER_DOLLAR))
                      .setScale(0, RoundingMode.HALF_UP)
                      .longValue();
    }

    /**
     * Convert cents to dollars.
     */
    public static BigDecimal toDollars(long cents) {
        return BigDecimal.valueOf(cents)
                         .divide(BigDecimal.valueOf(CENTS_PER_DOLLAR), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Format cents as display string (e.g., "25.00").
     */
    public static String formatCents(long cents) {
        return toDollars(cents).toPlainString();
    }

    /**
     * Format cents with currency symbol (e.g., "$25.00").
     */
    public static String formatWithSymbol(long cents, String currencyCode) {
        String symbol = getCurrencySymbol(currencyCode);
        return symbol + formatCents(cents);
    }

    /**
     * Get currency symbol for currency code.
     */
    public static String getCurrencySymbol(String currencyCode) {
        if (currencyCode == null) {
            return "$";
        }
        return switch (currencyCode.toUpperCase()) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "INR" -> "₹";
            case "JPY" -> "¥";
            default -> currencyCode + " ";
        };
    }

    /**
     * Check if an amount in cents is positive.
     */
    public static boolean isPositive(Long cents) {
        return cents != null && cents > 0;
    }

    /**
     * Check if an amount in cents is zero or null.
     */
    public static boolean isZeroOrNull(Long cents) {
        return cents == null || cents == 0;
    }

    /**
     * Calculate the remaining balance after refunds.
     */
    public static long calculateAvailableForRefund(long capturedAmount, long refundedAmount) {
        return Math.max(0, capturedAmount - refundedAmount);
    }

    /**
     * Calculate percentage of an amount.
     */
    public static long calculatePercentage(long amount, BigDecimal percentage) {
        return BigDecimal.valueOf(amount)
                         .multiply(percentage)
                         .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                         .longValue();
    }

    /**
     * Add amounts safely, treating null as zero.
     */
    public static long addSafe(Long... amounts) {
        long total = 0;
        for (Long amount : amounts) {
            if (amount != null) {
                total += amount;
            }
        }
        return total;
    }

    /**
     * Validate that an amount is within acceptable range.
     */
    public static boolean isValidAmount(Long cents, long minCents, long maxCents) {
        if (cents == null) {
            return false;
        }
        return cents >= minCents && cents <= maxCents;
    }

    /**
     * Calculate net amount (captured - refunded).
     */
    public static long calculateNetAmount(long capturedAmount, long refundedAmount) {
        return capturedAmount - refundedAmount;
    }
}
