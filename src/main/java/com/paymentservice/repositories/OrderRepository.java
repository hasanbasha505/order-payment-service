package com.paymentservice.repositories;

import com.paymentservice.enums.OrderStatus;
import com.paymentservice.models.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") UUID id);

    Page<Order> findByRestaurantId(UUID restaurantId, Pageable pageable);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.restaurantId = :restaurantId " +
           "AND o.createdAt >= :startTime AND o.createdAt < :endTime")
    Page<Order> findByRestaurantIdAndCreatedAtBetween(
        @Param("restaurantId") UUID restaurantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable
    );

    @Query("SELECT o FROM Order o JOIN FETCH o.payment WHERE o.id = :id")
    Optional<Order> findByIdWithPayment(@Param("id") UUID id);

    @Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.payment WHERE o.restaurantId = :restaurantId",
           countQuery = "SELECT count(o) FROM Order o WHERE o.restaurantId = :restaurantId")
    Page<Order> findByRestaurantIdWithPayment(@Param("restaurantId") UUID restaurantId, Pageable pageable);

    @Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.payment WHERE o.customerId = :customerId",
           countQuery = "SELECT count(o) FROM Order o WHERE o.customerId = :customerId")
    Page<Order> findByCustomerIdWithPayment(@Param("customerId") UUID customerId, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);
}
