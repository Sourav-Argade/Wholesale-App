package com.wholesale.controller;

import com.wholesale.dto.RouteDTO;
import com.wholesale.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public ResponseEntity<List<RouteDTO>> getAllRoutes() {
        return ResponseEntity.ok(routeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteDTO> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RouteDTO> createRoute(@RequestBody RouteDTO dto) {
        return ResponseEntity.ok(routeService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RouteDTO> updateRoute(@PathVariable Long id,
                                                 @RequestBody RouteDTO dto) {
        return ResponseEntity.ok(routeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{routeId}/customers")
    public ResponseEntity<RouteDTO> addCustomerToRoute(
            @PathVariable Long routeId,
            @RequestBody Map<String, Object> body) {
        Long customerId = Long.valueOf(body.get("customerId").toString());
        int order = body.containsKey("visitOrder") ? Integer.parseInt(body.get("visitOrder").toString()) : 0;
        return ResponseEntity.ok(routeService.addCustomerToRoute(routeId, customerId, order));
    }

    @DeleteMapping("/{routeId}/customers/{customerId}")
    public ResponseEntity<Void> removeCustomerFromRoute(
            @PathVariable Long routeId,
            @PathVariable Long customerId) {
        routeService.removeCustomerFromRoute(routeId, customerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{routeId}/customers/order")
    public ResponseEntity<Void> updateCustomerOrder(
            @PathVariable Long routeId,
            @RequestBody Map<String, List<Long>> body) {
        routeService.updateCustomerOrder(routeId, body.get("customerIds"));
        return ResponseEntity.ok().build();
    }
}
