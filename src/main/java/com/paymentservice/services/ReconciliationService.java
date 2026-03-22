package com.paymentservice.services;

import com.paymentservice.dto.request.GenerateReportRequest;
import com.paymentservice.dto.response.ReconciliationReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating and managing reconciliation reports.
 * Reconciliation aggregates captured/refunded amounts per restaurant per day,
 * using the restaurant's local timezone.
 */
public interface ReconciliationService {

    /**
     * Generate a reconciliation report for a restaurant on a specific date.
     * Uses the restaurant's timezone to determine the day boundaries.
     *
     * @param request Contains restaurantId and reportDate
     * @return Generated or existing report
     */
    ReconciliationReportResponse generateReport(GenerateReportRequest request);

    /**
     * Get a specific reconciliation report by ID.
     *
     * @param reportId Report ID
     * @return Report response
     */
    ReconciliationReportResponse getReport(UUID reportId);

    /**
     * List reconciliation reports for a restaurant.
     *
     * @param restaurantId Restaurant ID
     * @param pageable Pagination info
     * @return Page of reports
     */
    Page<ReconciliationReportResponse> getReportsByRestaurant(UUID restaurantId, Pageable pageable);

    /**
     * List reconciliation reports for a restaurant within a date range.
     *
     * @param restaurantId Restaurant ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param pageable Pagination info
     * @return Page of reports
     */
    Page<ReconciliationReportResponse> getReportsByDateRange(UUID restaurantId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Get all reports for a specific date across all restaurants.
     *
     * @param reportDate The date to query
     * @param pageable Pagination info
     * @return Page of reports
     */
    Page<ReconciliationReportResponse> getReportsByDate(LocalDate reportDate, Pageable pageable);

    /**
     * Get reports with mismatches that need investigation.
     *
     * @param pageable Pagination info
     * @return Page of unresolved mismatch reports
     */
    Page<ReconciliationReportResponse> getUnresolvedMismatches(Pageable pageable);

    /**
     * Mark a report as investigated.
     *
     * @param reportId Report ID
     * @param investigatedBy Investigator identifier
     * @return Updated report
     */
    ReconciliationReportResponse markAsInvestigated(UUID reportId, String investigatedBy);
}
