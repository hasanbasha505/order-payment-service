package com.paymentservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoneyUtil")
class MoneyUtilTest {

    @Nested
    @DisplayName("toCents")
    class ToCentsTests {

        @Test
        @DisplayName("should convert dollars to cents")
        void shouldConvertDollarsToCents() {
            assertThat(MoneyUtil.toCents(BigDecimal.valueOf(25.00))).isEqualTo(2500L);
            assertThat(MoneyUtil.toCents(BigDecimal.valueOf(10.50))).isEqualTo(1050L);
            assertThat(MoneyUtil.toCents(BigDecimal.valueOf(0.01))).isEqualTo(1L);
        }

        @Test
        @DisplayName("should handle null")
        void shouldHandleNull() {
            assertThat(MoneyUtil.toCents(null)).isEqualTo(0L);
        }

        @Test
        @DisplayName("should round correctly")
        void shouldRoundCorrectly() {
            assertThat(MoneyUtil.toCents(BigDecimal.valueOf(10.555))).isEqualTo(1056L);
            assertThat(MoneyUtil.toCents(BigDecimal.valueOf(10.554))).isEqualTo(1055L);
        }
    }

    @Nested
    @DisplayName("toDollars")
    class ToDollarsTests {

        @Test
        @DisplayName("should convert cents to dollars")
        void shouldConvertCentsToDollars() {
            assertThat(MoneyUtil.toDollars(2500L)).isEqualByComparingTo(BigDecimal.valueOf(25.00));
            assertThat(MoneyUtil.toDollars(1050L)).isEqualByComparingTo(BigDecimal.valueOf(10.50));
            assertThat(MoneyUtil.toDollars(1L)).isEqualByComparingTo(BigDecimal.valueOf(0.01));
        }
    }

    @Nested
    @DisplayName("formatCents")
    class FormatCentsTests {

        @Test
        @DisplayName("should format cents as string")
        void shouldFormatCentsAsString() {
            assertThat(MoneyUtil.formatCents(2500L)).isEqualTo("25.00");
            assertThat(MoneyUtil.formatCents(1050L)).isEqualTo("10.50");
            assertThat(MoneyUtil.formatCents(5L)).isEqualTo("0.05");
        }
    }

    @Nested
    @DisplayName("formatWithSymbol")
    class FormatWithSymbolTests {

        @Test
        @DisplayName("should format with currency symbol")
        void shouldFormatWithCurrencySymbol() {
            assertThat(MoneyUtil.formatWithSymbol(2500L, "USD")).isEqualTo("$25.00");
            assertThat(MoneyUtil.formatWithSymbol(2500L, "EUR")).isEqualTo("€25.00");
            assertThat(MoneyUtil.formatWithSymbol(2500L, "GBP")).isEqualTo("£25.00");
            assertThat(MoneyUtil.formatWithSymbol(2500L, "INR")).isEqualTo("₹25.00");
        }
    }

    @Nested
    @DisplayName("calculateAvailableForRefund")
    class CalculateAvailableForRefundTests {

        @Test
        @DisplayName("should calculate available amount")
        void shouldCalculateAvailableAmount() {
            assertThat(MoneyUtil.calculateAvailableForRefund(2500L, 0L)).isEqualTo(2500L);
            assertThat(MoneyUtil.calculateAvailableForRefund(2500L, 1000L)).isEqualTo(1500L);
            assertThat(MoneyUtil.calculateAvailableForRefund(2500L, 2500L)).isEqualTo(0L);
        }

        @Test
        @DisplayName("should return zero when refunded exceeds captured")
        void shouldReturnZeroWhenExceeds() {
            assertThat(MoneyUtil.calculateAvailableForRefund(2500L, 3000L)).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("isPositive")
    class IsPositiveTests {

        @Test
        @DisplayName("should detect positive amounts")
        void shouldDetectPositiveAmounts() {
            assertThat(MoneyUtil.isPositive(100L)).isTrue();
            assertThat(MoneyUtil.isPositive(1L)).isTrue();
            assertThat(MoneyUtil.isPositive(0L)).isFalse();
            assertThat(MoneyUtil.isPositive(-100L)).isFalse();
            assertThat(MoneyUtil.isPositive(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("addSafe")
    class AddSafeTests {

        @Test
        @DisplayName("should add amounts safely")
        void shouldAddAmountsSafely() {
            assertThat(MoneyUtil.addSafe(100L, 200L, 300L)).isEqualTo(600L);
            assertThat(MoneyUtil.addSafe(100L, null, 300L)).isEqualTo(400L);
            assertThat(MoneyUtil.addSafe(null, null, null)).isEqualTo(0L);
        }
    }
}
