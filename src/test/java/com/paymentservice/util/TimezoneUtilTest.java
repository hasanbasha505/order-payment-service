package com.paymentservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimezoneUtil")
class TimezoneUtilTest {

    private static final ZoneId NYC = ZoneId.of("America/New_York");
    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Nested
    @DisplayName("toLocalDate")
    class ToLocalDateTests {

        @Test
        @DisplayName("should convert instant to local date in timezone")
        void shouldConvertInstantToLocalDate() {
            // 2024-01-15 00:30:00 UTC = 2024-01-14 19:30:00 EST
            Instant instant = Instant.parse("2024-01-15T00:30:00Z");

            assertThat(TimezoneUtil.toLocalDate(instant, UTC))
                    .isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(TimezoneUtil.toLocalDate(instant, NYC))
                    .isEqualTo(LocalDate.of(2024, 1, 14));
        }

        @Test
        @DisplayName("should handle null inputs")
        void shouldHandleNullInputs() {
            assertThat(TimezoneUtil.toLocalDate(null, UTC)).isNull();
            assertThat(TimezoneUtil.toLocalDate(Instant.now(), null)).isNull();
        }
    }

    @Nested
    @DisplayName("startOfDay")
    class StartOfDayTests {

        @Test
        @DisplayName("should get start of day in timezone")
        void shouldGetStartOfDay() {
            LocalDate date = LocalDate.of(2024, 1, 15);

            Instant utcStart = TimezoneUtil.startOfDay(date, UTC);
            assertThat(utcStart).isEqualTo(Instant.parse("2024-01-15T00:00:00Z"));

            // NYC is UTC-5 in winter, so start of day is 05:00 UTC
            Instant nycStart = TimezoneUtil.startOfDay(date, NYC);
            assertThat(nycStart).isEqualTo(Instant.parse("2024-01-15T05:00:00Z"));
        }
    }

    @Nested
    @DisplayName("startOfNextDay")
    class StartOfNextDayTests {

        @Test
        @DisplayName("should get start of next day")
        void shouldGetStartOfNextDay() {
            LocalDate date = LocalDate.of(2024, 1, 15);

            Instant utcNextDay = TimezoneUtil.startOfNextDay(date, UTC);
            assertThat(utcNextDay).isEqualTo(Instant.parse("2024-01-16T00:00:00Z"));
        }
    }

    @Nested
    @DisplayName("getDayRange")
    class GetDayRangeTests {

        @Test
        @DisplayName("should get correct day range")
        void shouldGetCorrectDayRange() {
            LocalDate date = LocalDate.of(2024, 1, 15);

            TimezoneUtil.TimeRange range = TimezoneUtil.getDayRange(date, UTC);

            assertThat(range.start()).isEqualTo(Instant.parse("2024-01-15T00:00:00Z"));
            assertThat(range.end()).isEqualTo(Instant.parse("2024-01-16T00:00:00Z"));
        }

        @Test
        @DisplayName("should handle timezone offset")
        void shouldHandleTimezoneOffset() {
            LocalDate date = LocalDate.of(2024, 1, 15);

            TimezoneUtil.TimeRange range = TimezoneUtil.getDayRange(date, KOLKATA);

            // India is UTC+5:30, so Jan 15 in India starts at Jan 14 18:30 UTC
            assertThat(range.start()).isEqualTo(Instant.parse("2024-01-14T18:30:00Z"));
            assertThat(range.end()).isEqualTo(Instant.parse("2024-01-15T18:30:00Z"));
        }
    }

    @Nested
    @DisplayName("isOnDate")
    class IsOnDateTests {

        @Test
        @DisplayName("should check if instant is on date")
        void shouldCheckIfInstantIsOnDate() {
            LocalDate date = LocalDate.of(2024, 1, 15);
            Instant instant = Instant.parse("2024-01-15T12:00:00Z");

            assertThat(TimezoneUtil.isOnDate(instant, date, UTC)).isTrue();
            assertThat(TimezoneUtil.isOnDate(instant, date.minusDays(1), UTC)).isFalse();
        }
    }

    @Nested
    @DisplayName("parseZoneId")
    class ParseZoneIdTests {

        @Test
        @DisplayName("should parse valid timezone")
        void shouldParseValidTimezone() {
            assertThat(TimezoneUtil.parseZoneId("America/New_York", UTC)).isEqualTo(NYC);
        }

        @Test
        @DisplayName("should return default for invalid timezone")
        void shouldReturnDefaultForInvalid() {
            assertThat(TimezoneUtil.parseZoneId("Invalid/Zone", UTC)).isEqualTo(UTC);
            assertThat(TimezoneUtil.parseZoneId(null, UTC)).isEqualTo(UTC);
            assertThat(TimezoneUtil.parseZoneId("", UTC)).isEqualTo(UTC);
        }
    }
}
