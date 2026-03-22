package com.paymentservice.mappers;

import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.models.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for Order entity to OrderResponse DTO.
 *
 * Maps:
 * - id -> orderId
 * - restaurant.name -> restaurantName (if restaurant is loaded)
 * - Uses PaymentMapper for nested payment
 */
@Mapper(componentModel = "spring",
        uses = {PaymentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "order", target = "restaurantName", qualifiedByName = "getRestaurantName")
    OrderResponse toResponse(Order order);

    @Named("getRestaurantName")
    default String getRestaurantName(Order order) {
        if (order == null || order.getRestaurant() == null) {
            return null;
        }
        return order.getRestaurant().getName();
    }
}
