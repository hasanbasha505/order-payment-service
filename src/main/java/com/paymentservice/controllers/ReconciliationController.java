package com.paymentservice.controllers;

import com.paymentservice.dto.request.GenerateReportRequest;
import com.paymentservice.dto.response.ApiResponse;
import com.paymentservice.dto.response.ReconciliationReportResponse;
import com.paymentservice.services.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reconciliation Controller - RESTful endpoints for daily reconciliation reports.
 *
 * Extends BaseController for common response helper methods.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reconciliation", description = "Daily reconciliation report management")
public class ReconciliationController extends BaseController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/generate")
    @Operation(summary = "Generate reconciliation report",
               description = "Generates a daily reconciliation report for a restaurant. " +
                           "Uses the restaurant's timezone for day boundaries.")
    public ResponseEntity<ApiResponse<ReconciliationReportResponse>> generateReport(
            @Valid @RequestBody GenerateReportRequest request) {

        ReconciliationReportResponse response = reconciliationService.generateReport(request);
        return created(response, "Reconciliation report generated successfully");
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Get reconciliation report",
               description = "Retrieves a specific reconciliation report by ID")
    public ResponseEntity<ApiResponse<ReconciliationReportResponse>> getReport(
            @Parameter(description = "Report ID") @PathVariable UUID reportId) {

        ReconciliationReportResponse response = reconciliationService.getReport(reportId);
        return ok(response);
    }

    @GetMapping("/reports")
    @Operation(summary = "List reconciliation reports",
               description = "Lists reconciliation reports for a restaurant with pagination")
    public ResponseEntity<ApiResponse<Page<ReconciliationReportResponse>>> getReportsByRestaurant(
            @Parameter(description = "Restaurant ID") @RequestParam UUID restaurantId,
            @PageableDefault(size = 20, sort = "reportDate") Pageable pageable) {

        Page<ReconciliationReportResponse> reports = reconciliationService
                .getReportsByRestaurant(restaurantId, pageable);
        return ok(reports);
    }

    @GetMapping("/reports/date-range")
    @Operation(summary = "Get reports by date range",
               description = "Retrieves reconciliation reports for a restaurant within a date range")
    public ResponseEntity<ApiResponse<Page<ReconciliationReportResponse>>> getReportsByDateRange(
            @Parameter(description = "Restaurant ID") @RequestParam UUID restaurantId,
            @Parameter(description = "Start date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "reportDate") Pageable pageable) {

        Page<ReconciliationReportResponse> reports = reconciliationService
                .getReportsByDateRange(restaurantId, startDate, endDate, pageable);
        return ok(reports);
    }

    @GetMapping("/reports/by-date")
    @Operation(summary = "Get all reports for a date",
               description = "Retrieves all reconciliation reports for a specific date across all restaurants")
    public ResponseEntity<ApiResponse<Page<ReconciliationReportResponse>>> getReportsByDate(
            @Parameter(description = "Report date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @PageableDefault(size = 20, sort = "restaurantId") Pageable pageable) {

        Page<ReconciliationReportResponse> reports = reconciliationService.getReportsByDate(reportDate, pageable);
        return ok(reports);
    }

    @GetMapping("/reports/mismatches")
    @Operation(summary = "Get unresolved mismatches",
               description = "Retrieves reconciliation reports with unresolved discrepancies")
    public ResponseEntity<ApiResponse<Page<ReconciliationReportResponse>>> getUnresolvedMismatches(
            @PageableDefault(size = 20, sort = "reportDate") Pageable pageable) {

        Page<ReconciliationReportResponse> reports = reconciliationService.getUnresolvedMismatches(pageable);
        return ok(reports);
    }

    @PostMapping("/reports/{reportId}/investigate")
    @Operation(summary = "Mark report as investigated",
               description = "Marks a reconciliation report as investigated")
    public ResponseEntity<ApiResponse<ReconciliationReportResponse>> markAsInvestigated(
            @Parameter(description = "Report ID") @PathVariable UUID reportId,
            @Parameter(description = "Investigator identifier")
            @RequestParam @NotBlank(message = "Investigator identifier is required")
            @Size(min = 2, max = 100, message = "Investigator identifier must be 2-100 characters")
            String investigatedBy) {

        ReconciliationReportResponse response = reconciliationService
                .markAsInvestigated(reportId, investigatedBy);
        return ok(response, "Report marked as investigated");
    }
}
