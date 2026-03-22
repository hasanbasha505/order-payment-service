package com.paymentservice.repositories;

import com.paymentservice.models.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    List<Restaurant> findByActiveTrue();

    List<Restaurant> findByTimezone(String timezone);
}
