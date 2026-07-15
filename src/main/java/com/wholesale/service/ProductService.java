package com.wholesale.service;

import com.wholesale.dto.ProductDTO;
import com.wholesale.model.Product;
import com.wholesale.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDTO> findAll() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        return toDTO(product);
    }

    public ProductDTO create(ProductDTO dto) {
        Product product = new Product();
        updateFromDTO(product, dto);
        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    public ProductDTO update(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        updateFromDTO(product, dto);
        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    private ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setUnit(product.getUnit());
        dto.setDefaultPrice(product.getDefaultPrice());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }

    private void updateFromDTO(Product product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setUnit(dto.getUnit());
        product.setDefaultPrice(dto.getDefaultPrice());
    }
}
