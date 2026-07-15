package com.wholesale.dto;

import java.util.List;

public class RouteDTO {
    private Long id;
    private String name;
    private String description;
    private List<CustomerDTO> customers;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<CustomerDTO> getCustomers() { return customers; }
    public void setCustomers(List<CustomerDTO> customers) { this.customers = customers; }
}
