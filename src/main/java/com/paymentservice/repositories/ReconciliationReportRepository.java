package com.paymentservice.repositories;

import com.paymentservice.enums.MatchStatus;
import com.paymentservice.enums.ReconciliationStatus;
import com.paymentservice.models.ReconciliationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, UUID> {

    Optional<ReconciliationReport> findByRestaurantIdAndReportDate(UUID restaurantId, LocalDate reportDate);

    Page<ReconciliationReport> findByRestaurantId(UUID restaurantId, Pageable pageable);

    Page<ReconciliationReport> findByReportDate(LocalDate reportDate, Pageable pageable);

    Page<ReconciliationReport> findByStatus(ReconciliationStatus status, Pageable pageable);

    Page<ReconciliationReport> findByMatchStatus(MatchStatus matchStatus, Pageable pageable);

    @Query("""
        SELECT r FROM ReconciliationReport r
        WHERE r.restaurantId = :restaurantId
        AND r.reportDate >= :startDate
        AND r.reportDate <= :endDate
        ORDER BY r.reportDate DESC
        """)
    Page<ReconciliationReport> findByRestaurantIdAndDateRange(
        @Param("restaurantId") UUID restaurantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    /**
     * Find reports with discrepancies that need investigation.
     */
    @Query("SELECT r FROM ReconciliationReport r WHERE r.matchStatus = 'MISMATCH' AND r.investigatedAt IS NULL")
    Page<ReconciliationReport> findUnresolvedMismatches(Pageable pageable);

    /**
     * Check if report already exists for restaurant and date.
     */
    boolean existsByRestaurantIdAndReportDate(UUID restaurantId, LocalDate reportDate);
}
