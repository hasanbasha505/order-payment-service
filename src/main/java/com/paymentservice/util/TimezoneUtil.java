package com.paymentservice.util;

import java.time.*;

/**
 * Utility class for timezone-aware date/time operations.
 * Critical for reconciliation which requires restaurant-local dates.
 */
public final class TimezoneUtil {

    private TimezoneUtil() {
        // Utility class
    }

    /**
     * Convert an Instant to LocalDate in the specified timezone.
     */
    public static LocalDate toLocalDate(Instant instant, ZoneId zoneId) {
        if (instant == null || zoneId == null) {
            return null;
        }
        return instant.atZone(zoneId).toLocalDate();
    }

    /**
     * Get the start of day (00:00:00) for a date in the specified timezone, as an Instant.
     */
    public static Instant startOfDay(LocalDate date, ZoneId zoneId) {
        if (date == null || zoneId == null) {
            return null;
        }
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * Get the end of day (23:59:59.999999999) for a date in the specified timezone, as an Instant.
     */
    public static Instant endOfDay(LocalDate date, ZoneId zoneId) {
        if (date == null || zoneId == null) {
            return null;
        }
        return date.atTime(LocalTime.MAX).atZone(zoneId).toInstant();
    }

    /**
     * Get the start of the next day for a date in the specified timezone, as an Instant.
     * Useful for exclusive end bounds in queries.
     */
    public static Instant startOfNextDay(LocalDate date, ZoneId zoneId) {
        if (date == null || zoneId == null) {
            return null;
        }
        return date.plusDays(1).atStartOfDay(zoneId).toInstant();
    }

    /**
     * Get the current local date for a timezone.
     */
    public static LocalDate currentLocalDate(ZoneId zoneId) {
        return LocalDate.now(zoneId);
    }

    /**
     * Check if an Instant falls on a specific date in a timezone.
     */
    public static boolean isOnDate(Instant instant, LocalDate date, ZoneId zoneId) {
        if (instant == null || date == null || zoneId == null) {
            return false;
        }
        LocalDate instantDate = toLocalDate(instant, zoneId);
        return date.equals(instantDate);
    }

    /**
     * Get yesterday's date for a timezone.
     */
    public static LocalDate yesterday(ZoneId zoneId) {
        return LocalDate.now(zoneId).minusDays(1);
    }

    /**
     * Parse timezone string safely.
     * Returns default timezone if invalid.
     */
    public static ZoneId parseZoneId(String timezone, ZoneId defaultZone) {
        if (timezone == null || timezone.isBlank()) {
            return defaultZone;
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            return defaultZone;
        }
    }

    /**
     * Get the time range (start, end) for a day in a specific timezone.
     * Returns [startOfDay, startOfNextDay) for use in database queries.
     */
    public static TimeRange getDayRange(LocalDate date, ZoneId zoneId) {
        return new TimeRange(
            startOfDay(date, zoneId),
            startOfNextDay(date, zoneId)
        );
    }

    /**
     * Represents a time range with start (inclusive) and end (exclusive).
     */
    public record TimeRange(Instant start, Instant end) {
    }
}
