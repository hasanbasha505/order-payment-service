package com.paymentservice.services;

import com.paymentservice.dto.request.GenerateReportRequest;
import com.paymentservice.dto.response.ReconciliationReportResponse;
import com.paymentservice.enums.MatchStatus;
import com.paymentservice.enums.PaymentStatus;
import com.paymentservice.enums.ReconciliationStatus;
import com.paymentservice.exceptions.OrderNotFoundException;
import com.paymentservice.models.Payment;
import com.paymentservice.models.ReconciliationReport;
import com.paymentservice.models.Restaurant;
import com.paymentservice.repositories.PaymentRepository;
import com.paymentservice.repositories.ReconciliationReportRepository;
import com.paymentservice.repositories.RestaurantRepository;
import com.paymentservice.services.impl.ReconciliationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationService")
class ReconciliationServiceTest {

    @Mock
    private ReconciliationReportRepository reconciliationReportRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private ReconciliationServiceImpl reconciliationService;

    private Restaurant testRestaurant;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        testRestaurant = Restaurant.builder()
                .id(restaurantId)
                .name("Test Restaurant")
                .timezone("America/New_York")
                .currencyCode("USD")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("generateReport")
    class GenerateReportTests {

        @Test
        @DisplayName("should generate new report when none exists")
        void shouldGenerateNewReport() {
            LocalDate reportDate = LocalDate.of(2024, 1, 15);
            GenerateReportRequest request = GenerateReportRequest.builder()
                    .restaurantId(restaurantId)
                    .reportDate(reportDate)
                    .build();

            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(testRestaurant));
            when(reconciliationReportRepository.findByRestaurantIdAndReportDate(restaurantId, reportDate))
                    .thenReturn(Optional.empty());

            List<Payment> payments = List.of(
                    createPayment(2500L, 0L, PaymentStatus.CAPTURED),
                    createPayment(1500L, 500L, PaymentStatus.PARTIALLY_REFUNDED)
            );
            when(paymentRepository.findForReconciliation(eq(restaurantId), any(), any()))
                    .thenReturn(payments);

            when(reconciliationReportRepository.save(any())).thenAnswer(inv -> {
                ReconciliationReport report = inv.getArgument(0);
                report.setId(UUID.randomUUID());
                return report;
            });

            ReconciliationReportResponse response = reconciliationService.generateReport(request);

            assertThat(response).isNotNull();
            assertThat(response.getTotalOrders()).isEqualTo(2);
            assertThat(response.getTotalCapturedAmount()).isEqualTo(4000L);  // 2500 + 1500
            assertThat(response.getTotalRefundedAmount()).isEqualTo(500L);
            assertThat(response.getNetAmount()).isEqualTo(3500L);  // 4000 - 500
            assertThat(response.getCurrencyCode()).isEqualTo("USD");
            assertThat(response.getStatus()).isEqualTo(ReconciliationStatus.GENERATED);
        }

        @Test
        @DisplayName("should return existing report if already generated")
        void shouldReturnExistingReport() {
            LocalDate reportDate = LocalDate.of(2024, 1, 15);
            GenerateReportRequest request = GenerateReportRequest.builder()
                    .restaurantId(restaurantId)
                    .reportDate(reportDate)
                    .build();

            ReconciliationReport existingReport = ReconciliationReport.builder()
                    .id(UUID.randomUUID())
                    .restaurantId(restaurantId)
                    .reportDate(reportDate)
                    .totalOrders(5)
                    .totalCapturedAmount(10000L)
                    .totalRefundedAmount(1000L)
                    .netAmount(9000L)
                    .currencyCode("USD")
                    .status(ReconciliationStatus.GENERATED)
                    .build();

            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(testRestaurant));
            when(reconciliationReportRepository.findByRestaurantIdAndReportDate(restaurantId, reportDate))
                    .thenReturn(Optional.of(existingReport));

            ReconciliationReportResponse response = reconciliationService.generateReport(request);

            assertThat(response.getTotalOrders()).isEqualTo(5);
            verify(paymentRepository, never()).findForReconciliation(any(), any(), any());
        }

        @Test
        @DisplayName("should throw when restaurant not found")
        void shouldThrowWhenRestaurantNotFound() {
            GenerateReportRequest request = GenerateReportRequest.builder()
                    .restaurantId(UUID.randomUUID())
                    .reportDate(LocalDate.now())
                    .build();

            when(restaurantRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reconciliationService.generateReport(request))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("should compare with provider when requested")
        void shouldCompareWithProvider() {
            LocalDate reportDate = LocalDate.of(2024, 1, 15);
            GenerateReportRequest request = GenerateReportRequest.builder()
                    .restaurantId(restaurantId)
                    .reportDate(reportDate)
                    .compareWithProvider(true)
                    .build();

            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(testRestaurant));
            when(reconciliationReportRepository.findByRestaurantIdAndReportDate(restaurantId, reportDate))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findForReconciliation(eq(restaurantId), any(), any()))
                    .thenReturn(List.of(createPayment(2500L, 0L, PaymentStatus.CAPTURED)));
            when(reconciliationReportRepository.save(any())).thenAnswer(inv -> {
                ReconciliationReport report = inv.getArgument(0);
                report.setId(UUID.randomUUID());
                return report;
            });

            ReconciliationReportResponse response = reconciliationService.generateReport(request);

            assertThat(response.getMatchStatus()).isEqualTo(MatchStatus.MATCHED);
            assertThat(response.getProviderNetAmount()).isEqualTo(response.getNetAmount());
        }
    }

    @Nested
    @DisplayName("markAsInvestigated")
    class MarkAsInvestigatedTests {

        @Test
        @DisplayName("should mark report as investigated")
        void shouldMarkAsInvestigated() {
            UUID reportId = UUID.randomUUID();
            ReconciliationReport report = ReconciliationReport.builder()
                    .id(reportId)
                    .restaurantId(restaurantId)
                    .reportDate(LocalDate.now())
                    .status(ReconciliationStatus.GENERATED)
                    .matchStatus(MatchStatus.MISMATCH)
                    .currencyCode("USD")
                    .build();

            when(reconciliationReportRepository.findById(reportId)).thenReturn(Optional.of(report));
            when(reconciliationReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(testRestaurant));

            ReconciliationReportResponse response = reconciliationService.markAsInvestigated(reportId, "user@example.com");

            assertThat(response.getStatus()).isEqualTo(ReconciliationStatus.VERIFIED);

            ArgumentCaptor<ReconciliationReport> captor = ArgumentCaptor.forClass(ReconciliationReport.class);
            verify(reconciliationReportRepository).save(captor.capture());

            ReconciliationReport saved = captor.getValue();
            assertThat(saved.getInvestigatedBy()).isEqualTo("user@example.com");
            assertThat(saved.getInvestigatedAt()).isNotNull();
        }
    }

    private Payment createPayment(long capturedAmount, long refundedAmount, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setCapturedAmount(capturedAmount);
        payment.setRefundedAmount(refundedAmount);
        payment.setStatus(status);
        payment.setCapturedAt(Instant.now());
        return payment;
    }
}
