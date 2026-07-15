package com.wholesale.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VisitLogDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerShopName;
    private Long userId;
    private String userName;
    private LocalDate visitedDate;
    private String notes;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerShopName() { return customerShopName; }
    public void setCustomerShopName(String customerShopName) { this.customerShopName = customerShopName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDate getVisitedDate() { return visitedDate; }
    public void setVisitedDate(LocalDate visitedDate) { this.visitedDate = visitedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
