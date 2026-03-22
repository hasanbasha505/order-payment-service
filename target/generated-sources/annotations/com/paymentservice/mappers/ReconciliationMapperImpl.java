package com.paymentservice.mappers;

import com.paymentservice.dto.response.ReconciliationReportResponse;
import com.paymentservice.models.ReconciliationReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-22T23:06:06+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Ubuntu)"
)
@Component
public class ReconciliationMapperImpl implements ReconciliationMapper {

    @Override
    public ReconciliationReportResponse toResponse(ReconciliationReport report) {
        if ( report == null ) {
            return null;
        }

        ReconciliationReportResponse.ReconciliationReportResponseBuilder<?, ?> reconciliationReportResponse = ReconciliationReportResponse.builder();

        reconciliationReportResponse.reportId( report.getId() );
        reconciliationReportResponse.restaurantName( getRestaurantName( report ) );
        reconciliationReportResponse.variance( calculateVariance( report ) );
        reconciliationReportResponse.createdAt( report.getCreatedAt() );
        reconciliationReportResponse.updatedAt( report.getUpdatedAt() );
        reconciliationReportResponse.restaurantId( report.getRestaurantId() );
        reconciliationReportResponse.reportDate( report.getReportDate() );
        reconciliationReportResponse.totalOrders( report.getTotalOrders() );
        reconciliationReportResponse.totalCapturedAmount( report.getTotalCapturedAmount() );
        reconciliationReportResponse.totalRefundedAmount( report.getTotalRefundedAmount() );
        reconciliationReportResponse.netAmount( report.getNetAmount() );
        reconciliationReportResponse.providerTotalCaptured( report.getProviderTotalCaptured() );
        reconciliationReportResponse.providerTotalRefunded( report.getProviderTotalRefunded() );
        reconciliationReportResponse.providerNetAmount( report.getProviderNetAmount() );
        reconciliationReportResponse.currencyCode( report.getCurrencyCode() );
        reconciliationReportResponse.status( report.getStatus() );
        reconciliationReportResponse.matchStatus( report.getMatchStatus() );
        Map<String, Object> map = report.getDiscrepancies();
        if ( map != null ) {
            reconciliationReportResponse.discrepancies( new LinkedHashMap<String, Object>( map ) );
        }
        reconciliationReportResponse.generatedAt( report.getGeneratedAt() );

        return reconciliationReportResponse.build();
    }

    @Override
    public List<ReconciliationReportResponse> toResponseList(List<ReconciliationReport> reports) {
        if ( reports == null ) {
            return null;
        }

        List<ReconciliationReportResponse> list = new ArrayList<ReconciliationReportResponse>( reports.size() );
        for ( ReconciliationReport reconciliationReport : reports ) {
            list.add( toResponse( reconciliationReport ) );
        }

        return list;
    }
}
