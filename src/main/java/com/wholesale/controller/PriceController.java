package com.wholesale.controller;

import com.wholesale.dto.CustomerPriceDTO;
import com.wholesale.service.PriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerPriceDTO>> getPriceHistory(
            @RequestParam Long customerId,
            @RequestParam(required = false) Long productId) {
        return ResponseEntity.ok(priceService.getPriceHistory(customerId, productId));
    }

    @PostMapping
    public ResponseEntity<CustomerPriceDTO> setPrice(@RequestBody CustomerPriceDTO dto) {
        return ResponseEntity.ok(priceService.setPrice(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrice(@PathVariable Long id) {
        priceService.deletePrice(id);
        return ResponseEntity.ok().build();
    }
}
