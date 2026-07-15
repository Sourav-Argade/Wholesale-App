package com.wholesale.repository;

import com.wholesale.model.RouteCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteCustomerRepository extends JpaRepository<RouteCustomer, Long> {

    List<RouteCustomer> findByRouteIdOrderByVisitOrderAsc(Long routeId);

    List<RouteCustomer> findByCustomerId(Long customerId);

    void deleteByRouteId(Long routeId);
}
