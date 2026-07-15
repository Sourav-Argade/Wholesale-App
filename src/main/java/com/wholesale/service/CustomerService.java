package com.wholesale.service;

import com.wholesale.dto.CustomerDTO;
import com.wholesale.dto.CustomerPriceDTO;
import com.wholesale.model.Customer;
import com.wholesale.model.CustomerPrice;
import com.wholesale.model.Product;
import com.wholesale.model.VisitLog;
import com.wholesale.repository.CustomerPriceRepository;
import com.wholesale.repository.CustomerRepository;
import com.wholesale.repository.ProductRepository;
import com.wholesale.repository.VisitLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerPriceRepository customerPriceRepository;
    private final ProductRepository productRepository;
    private final VisitLogRepository visitLogRepository;
    private final GoogleSheetsService googleSheetsService;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerPriceRepository customerPriceRepository,
                           ProductRepository productRepository,
                           VisitLogRepository visitLogRepository,
                           GoogleSheetsService googleSheetsService) {
        this.customerRepository = customerRepository;
        this.customerPriceRepository = customerPriceRepository;
        this.productRepository = productRepository;
        this.visitLogRepository = visitLogRepository;
        this.googleSheetsService = googleSheetsService;
    }

    public List<CustomerDTO> findAll() {
        return customerRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CustomerDTO findById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        return toDTO(customer);
    }

    public CustomerDTO create(CustomerDTO dto, Long userId) {
        Customer customer = new Customer();
        updateFromDTO(customer, dto);
        customer.setCreatedBy(userId);
        Customer saved = customerRepository.save(customer);

        // Sync to Google Sheets
        googleSheetsService.syncCustomer(
                saved.getName(), saved.getShopName(), saved.getPhone(),
                saved.getAddress(),
                saved.getLatitude() != null ? saved.getLatitude().toString() : null,
                saved.getLongitude() != null ? saved.getLongitude().toString() : null,
                null, saved.getNotes());

        return toDTO(saved);
    }

    public CustomerDTO update(Long id, CustomerDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        updateFromDTO(customer, dto);
        Customer saved = customerRepository.save(customer);

        // Sync to Google Sheets
        googleSheetsService.syncCustomer(
                saved.getName(), saved.getShopName(), saved.getPhone(),
                saved.getAddress(),
                saved.getLatitude() != null ? saved.getLatitude().toString() : null,
                saved.getLongitude() != null ? saved.getLongitude().toString() : null,
                null, saved.getNotes());

        return toDTO(saved);
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }

    public List<CustomerDTO> search(String query) {
        return customerRepository.search(query).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private CustomerDTO toDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setShopName(customer.getShopName());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setLatitude(customer.getLatitude());
        dto.setLongitude(customer.getLongitude());
        dto.setNotes(customer.getNotes());
        dto.setCreatedBy(customer.getCreatedBy());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());

        // Attach latest prices
        Map<String, Object> latestPrices = new HashMap<>();
        List<Product> allProducts = productRepository.findAll();
        for (Product product : allProducts) {
            Optional<CustomerPrice> latestPrice = customerPriceRepository
                    .findLatestPrice(customer.getId(), product.getId());
            if (latestPrice.isPresent()) {
                Map<String, Object> priceInfo = new HashMap<>();
                CustomerPrice cp = latestPrice.get();
                priceInfo.put("productId", product.getId());
                priceInfo.put("productName", product.getName());
                priceInfo.put("productUnit", product.getUnit());
                priceInfo.put("price", cp.getPrice());
                priceInfo.put("effectiveDate", cp.getEffectiveDate().toString());
                latestPrices.put(product.getId().toString(), priceInfo);
            }
        }
        dto.setLatestPrices(latestPrices);

        // Last visit date
        List<VisitLog> visits = visitLogRepository.findByCustomerIdOrderByVisitedDateDesc(customer.getId());
        if (!visits.isEmpty()) {
            dto.setLastVisitDate(visits.get(0).getVisitedDate());
        }

        return dto;
    }

    private void updateFromDTO(Customer customer, CustomerDTO dto) {
        customer.setName(dto.getName());
        customer.setShopName(dto.getShopName());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setLatitude(dto.getLatitude());
        customer.setLongitude(dto.getLongitude());
        customer.setNotes(dto.getNotes());
    }
}
