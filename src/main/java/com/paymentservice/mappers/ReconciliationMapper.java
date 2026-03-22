package com.paymentservice.mappers;

import com.paymentservice.dto.response.ReconciliationReportResponse;
import com.paymentservice.models.ReconciliationReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for ReconciliationReport entity to ReconciliationReportResponse DTO.
 *
 * Maps:
 * - id -> reportId
 * - restaurant.name -> restaurantName (if restaurant is loaded)
 * - Calculates variance from entity method
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReconciliationMapper {

    @Mapping(source = "id", target = "reportId")
    @Mapping(source = "report", target = "restaurantName", qualifiedByName = "getRestaurantName")
    @Mapping(source = "report", target = "variance", qualifiedByName = "calculateVariance")
    ReconciliationReportResponse toResponse(ReconciliationReport report);

    List<ReconciliationReportResponse> toResponseList(List<ReconciliationReport> reports);

    @Named("getRestaurantName")
    default String getRestaurantName(ReconciliationReport report) {
        if (report == null || report.getRestaurant() == null) {
            return null;
        }
        return report.getRestaurant().getName();
    }

    @Named("calculateVariance")
    default Long calculateVariance(ReconciliationReport report) {
        if (report == null) {
            return null;
        }
        return report.getVariance();
    }
}
