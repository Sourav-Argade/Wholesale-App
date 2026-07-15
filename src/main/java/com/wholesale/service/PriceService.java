package com.wholesale.service;

import com.wholesale.dto.CustomerPriceDTO;
import com.wholesale.model.CustomerPrice;
import com.wholesale.repository.CustomerPriceRepository;
import com.wholesale.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PriceService {

    private final CustomerPriceRepository priceRepository;
    private final ProductRepository productRepository;

    public PriceService(CustomerPriceRepository priceRepository,
                        ProductRepository productRepository) {
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
    }

    public List<CustomerPriceDTO> getPriceHistory(Long customerId, Long productId) {
        List<CustomerPrice> prices;
        if (productId != null) {
            prices = priceRepository.findByCustomerIdAndProductIdOrderByEffectiveDateDesc(customerId, productId);
        } else {
            prices = priceRepository.findByCustomerIdOrderByEffectiveDateDesc(customerId);
        }
        return prices.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public CustomerPriceDTO setPrice(CustomerPriceDTO dto) {
        CustomerPrice price = new CustomerPrice();
        price.setCustomerId(dto.getCustomerId());
        price.setProductId(dto.getProductId());
        price.setPrice(dto.getPrice());
        price.setEffectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDate.now());
        price.setNotes(dto.getNotes());
        CustomerPrice saved = priceRepository.save(price);
        return toDTO(saved);
    }

    public void deletePrice(Long id) {
        priceRepository.deleteById(id);
    }

    private CustomerPriceDTO toDTO(CustomerPrice price) {
        CustomerPriceDTO dto = new CustomerPriceDTO();
        dto.setId(price.getId());
        dto.setCustomerId(price.getCustomerId());
        dto.setProductId(price.getProductId());
        dto.setPrice(price.getPrice());
        dto.setEffectiveDate(price.getEffectiveDate());
        dto.setNotes(price.getNotes());

        // Enrich with product info
        productRepository.findById(price.getProductId()).ifPresent(product -> {
            dto.setProductName(product.getName());
            dto.setProductUnit(product.getUnit());
        });

        return dto;
    }
}
