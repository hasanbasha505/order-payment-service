package com.paymentservice.services.impl;

import com.paymentservice.dto.request.GenerateReportRequest;
import com.paymentservice.dto.response.ReconciliationReportResponse;
import com.paymentservice.enums.MatchStatus;
import com.paymentservice.enums.ReconciliationStatus;
import com.paymentservice.exceptions.OrderNotFoundException;
import com.paymentservice.models.Payment;
import com.paymentservice.models.ReconciliationReport;
import com.paymentservice.models.Restaurant;
import com.paymentservice.repositories.PaymentRepository;
import com.paymentservice.repositories.ReconciliationReportRepository;
import com.paymentservice.repositories.RestaurantRepository;
import com.paymentservice.services.ReconciliationService;
import com.paymentservice.util.TimezoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ReconciliationReportRepository reconciliationReportRepository;
    private final PaymentRepository paymentRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    @Transactional
    public ReconciliationReportResponse generateReport(GenerateReportRequest request) {
        log.info("Generating reconciliation report for restaurant={}, date={}",
                request.getRestaurantId(), request.getReportDate());

        // Get restaurant for timezone info
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Restaurant not found: " + request.getRestaurantId()));

        ZoneId zoneId = restaurant.getZoneId();

        // Check if report already exists
        Optional<ReconciliationReport> existing = reconciliationReportRepository
                .findByRestaurantIdAndReportDate(request.getRestaurantId(), request.getReportDate());

        if (existing.isPresent()) {
            log.info("Returning existing report: {}", existing.get().getId());
            return mapToResponse(existing.get(), restaurant);
        }

        // Calculate time range for the day in restaurant's timezone
        TimezoneUtil.TimeRange dayRange = TimezoneUtil.getDayRange(request.getReportDate(), zoneId);

        // Fetch all payments for this restaurant on this day
        List<Payment> payments = paymentRepository.findForReconciliation(
                request.getRestaurantId(),
                dayRange.start(),
                dayRange.end());

        // Aggregate totals
        int totalOrders = payments.size();
        long totalCaptured = 0L;
        long totalRefunded = 0L;

        List<Map<String, Object>> paymentDetails = new ArrayList<>();

        for (Payment payment : payments) {
            totalCaptured += payment.getCapturedAmount() != null ? payment.getCapturedAmount() : 0;
            totalRefunded += payment.getRefundedAmount() != null ? payment.getRefundedAmount() : 0;

            // Store details for report data
            Map<String, Object> detail = new HashMap<>();
            detail.put("paymentId", payment.getId().toString());
            detail.put("orderId", payment.getOrderId().toString());
            detail.put("capturedAmount", payment.getCapturedAmount());
            detail.put("refundedAmount", payment.getRefundedAmount());
            detail.put("status", payment.getStatus().name());
            detail.put("capturedAt", payment.getCapturedAt() != null ? payment.getCapturedAt().toString() : null);
            paymentDetails.add(detail);
        }

        long netAmount = totalCaptured - totalRefunded;

        // Build report data JSON
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("payments", paymentDetails);
        reportData.put("timezone", restaurant.getTimezone());
        reportData.put("generatedAt", Instant.now().toString());

        // Create the report
        ReconciliationReport report = ReconciliationReport.builder()
                .restaurantId(request.getRestaurantId())
                .reportDate(request.getReportDate())
                .totalOrders(totalOrders)
                .totalCapturedAmount(totalCaptured)
                .totalRefundedAmount(totalRefunded)
                .netAmount(netAmount)
                .currencyCode(restaurant.getCurrencyCode())
                .status(ReconciliationStatus.GENERATED)
                .matchStatus(MatchStatus.PENDING)
                .reportData(reportData)
                .generatedAt(Instant.now())
                .build();

        // If comparing with provider, set match status
        if (request.isCompareWithProvider()) {
            // In a real implementation, this would fetch data from the payment provider
            // For now, we'll simulate a match
            report.setProviderTotalCaptured(totalCaptured);
            report.setProviderTotalRefunded(totalRefunded);
            report.setProviderNetAmount(netAmount);
            report.setMatchStatus(MatchStatus.MATCHED);
        }

        report = reconciliationReportRepository.save(report);
        log.info("Created reconciliation report: {}", report.getId());

        return mapToResponse(report, restaurant);
    }

    @Override
    public ReconciliationReportResponse getReport(UUID reportId) {
        ReconciliationReport report = reconciliationReportRepository.findById(reportId)
                .orElseThrow(() -> new OrderNotFoundException("Report not found: " + reportId));

        Restaurant restaurant = restaurantRepository.findById(report.getRestaurantId())
                .orElse(null);

        return mapToResponse(report, restaurant);
    }

    @Override
    public Page<ReconciliationReportResponse> getReportsByRestaurant(UUID restaurantId, Pageable pageable) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);

        return reconciliationReportRepository.findByRestaurantId(restaurantId, pageable)
                .map(report -> mapToResponse(report, restaurant));
    }

    @Override
    public Page<ReconciliationReportResponse> getReportsByDateRange(
            UUID restaurantId, LocalDate startDate, LocalDate endDate, Pageable pageable) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);

        return reconciliationReportRepository
                .findByRestaurantIdAndDateRange(restaurantId, startDate, endDate, pageable)
                .map(report -> mapToResponse(report, restaurant));
    }

    @Override
    public Page<ReconciliationReportResponse> getReportsByDate(LocalDate reportDate, Pageable pageable) {
        return reconciliationReportRepository.findByReportDate(reportDate, pageable)
                .map(report -> {
                    Restaurant restaurant = restaurantRepository.findById(report.getRestaurantId()).orElse(null);
                    return mapToResponse(report, restaurant);
                });
    }

    @Override
    public Page<ReconciliationReportResponse> getUnresolvedMismatches(Pageable pageable) {
        return reconciliationReportRepository.findUnresolvedMismatches(pageable)
                .map(report -> {
                    Restaurant restaurant = restaurantRepository.findById(report.getRestaurantId()).orElse(null);
                    return mapToResponse(report, restaurant);
                });
    }

    @Override
    @Transactional
    public ReconciliationReportResponse markAsInvestigated(UUID reportId, String investigatedBy) {
        ReconciliationReport report = reconciliationReportRepository.findById(reportId)
                .orElseThrow(() -> new OrderNotFoundException("Report not found: " + reportId));

        report.setInvestigatedAt(Instant.now());
        report.setInvestigatedBy(investigatedBy);
        report.setStatus(ReconciliationStatus.VERIFIED);

        report = reconciliationReportRepository.save(report);

        Restaurant restaurant = restaurantRepository.findById(report.getRestaurantId()).orElse(null);

        return mapToResponse(report, restaurant);
    }

    private ReconciliationReportResponse mapToResponse(ReconciliationReport report, Restaurant restaurant) {
        return ReconciliationReportResponse.builder()
                .reportId(report.getId())
                .restaurantId(report.getRestaurantId())
                .restaurantName(restaurant != null ? restaurant.getName() : null)
                .reportDate(report.getReportDate())
                .totalOrders(report.getTotalOrders())
                .totalCapturedAmount(report.getTotalCapturedAmount())
                .totalRefundedAmount(report.getTotalRefundedAmount())
                .netAmount(report.getNetAmount())
                .providerTotalCaptured(report.getProviderTotalCaptured())
                .providerTotalRefunded(report.getProviderTotalRefunded())
                .providerNetAmount(report.getProviderNetAmount())
                .variance(report.getVariance())
                .currencyCode(report.getCurrencyCode())
                .status(report.getStatus())
                .matchStatus(report.getMatchStatus())
                .discrepancies(report.getDiscrepancies())
                .generatedAt(report.getGeneratedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
