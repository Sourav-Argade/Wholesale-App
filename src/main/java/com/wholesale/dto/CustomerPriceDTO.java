package com.wholesale.dto;

import java.util.List;

public class CustomerPriceDTO {
    private Long id;
    private Long customerId;
    private Long productId;
    private String productName;
    private String productUnit;
    private java.math.BigDecimal price;
    private java.time.LocalDate effectiveDate;
    private String notes;

    // For bulk operations
    private List<CustomerPriceDTO> prices;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductUnit() { return productUnit; }
    public void setProductUnit(String productUnit) { this.productUnit = productUnit; }

    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }

    public java.time.LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(java.time.LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<CustomerPriceDTO> getPrices() { return prices; }
    public void setPrices(List<CustomerPriceDTO> prices) { this.prices = prices; }
}
