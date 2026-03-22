package com.paymentservice.mappers;

import com.paymentservice.dto.response.OrderResponse;
import com.paymentservice.models.Order;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-22T23:06:06+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Ubuntu)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Autowired
    private PaymentMapper paymentMapper;

    @Override
    public OrderResponse toResponse(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderResponse.OrderResponseBuilder<?, ?> orderResponse = OrderResponse.builder();

        orderResponse.orderId( order.getId() );
        orderResponse.restaurantName( getRestaurantName( order ) );
        orderResponse.createdAt( order.getCreatedAt() );
        orderResponse.updatedAt( order.getUpdatedAt() );
        orderResponse.orderNumber( order.getOrderNumber() );
        orderResponse.restaurantId( order.getRestaurantId() );
        orderResponse.customerId( order.getCustomerId() );
        orderResponse.status( order.getStatus() );
        orderResponse.totalAmount( order.getTotalAmount() );
        orderResponse.currencyCode( order.getCurrencyCode() );
        Map<String, Object> map = order.getMetadata();
        if ( map != null ) {
            orderResponse.metadata( new LinkedHashMap<String, Object>( map ) );
        }
        orderResponse.payment( paymentMapper.toResponse( order.getPayment() ) );

        return orderResponse.build();
    }
}
