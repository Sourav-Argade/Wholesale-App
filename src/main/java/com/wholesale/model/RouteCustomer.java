package com.wholesale.model;

import jakarta.persistence.*;

@Entity
@Table(name = "route_customers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"route_id", "customer_id"}))
public class RouteCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "visit_order", nullable = false)
    private int visitOrder = 0;

    public RouteCustomer() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public int getVisitOrder() { return visitOrder; }
    public void setVisitOrder(int visitOrder) { this.visitOrder = visitOrder; }
}
