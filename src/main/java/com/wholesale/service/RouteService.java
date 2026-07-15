package com.wholesale.service;

import com.wholesale.dto.CustomerDTO;
import com.wholesale.dto.RouteDTO;
import com.wholesale.model.Route;
import com.wholesale.model.RouteCustomer;
import com.wholesale.repository.CustomerRepository;
import com.wholesale.repository.RouteCustomerRepository;
import com.wholesale.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteCustomerRepository routeCustomerRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    public RouteService(RouteRepository routeRepository,
                        RouteCustomerRepository routeCustomerRepository,
                        CustomerRepository customerRepository,
                        CustomerService customerService) {
        this.routeRepository = routeRepository;
        this.routeCustomerRepository = routeCustomerRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
    }

    public List<RouteDTO> findAll() {
        List<Route> routes = routeRepository.findAll();
        List<RouteDTO> dtos = new ArrayList<>();
        for (Route route : routes) {
            dtos.add(toDTO(route));
        }
        return dtos;
    }

    public RouteDTO findById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));
        return toDTO(route);
    }

    public RouteDTO create(RouteDTO dto) {
        Route route = new Route();
        route.setName(dto.getName());
        route.setDescription(dto.getDescription());
        Route saved = routeRepository.save(route);
        return toDTO(saved);
    }

    public RouteDTO update(Long id, RouteDTO dto) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));
        route.setName(dto.getName());
        route.setDescription(dto.getDescription());
        Route saved = routeRepository.save(route);
        return toDTO(saved);
    }

    public void delete(Long id) {
        routeCustomerRepository.deleteByRouteId(id);
        routeRepository.deleteById(id);
    }

    public RouteDTO addCustomerToRoute(Long routeId, Long customerId, int visitOrder) {
        RouteCustomer rc = new RouteCustomer();
        rc.setRouteId(routeId);
        rc.setCustomerId(customerId);
        rc.setVisitOrder(visitOrder);
        routeCustomerRepository.save(rc);

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));
        return toDTO(route);
    }

    public void removeCustomerFromRoute(Long routeId, Long customerId) {
        List<RouteCustomer> rcs = routeCustomerRepository.findByRouteIdOrderByVisitOrderAsc(routeId);
        rcs.stream()
            .filter(rc -> rc.getCustomerId().equals(customerId))
            .findFirst()
            .ifPresent(routeCustomerRepository::delete);
    }

    public void updateCustomerOrder(Long routeId, List<Long> customerIdsInOrder) {
        routeCustomerRepository.deleteByRouteId(routeId);
        for (int i = 0; i < customerIdsInOrder.size(); i++) {
            RouteCustomer rc = new RouteCustomer();
            rc.setRouteId(routeId);
            rc.setCustomerId(customerIdsInOrder.get(i));
            rc.setVisitOrder(i + 1);
            routeCustomerRepository.save(rc);
        }
    }

    private RouteDTO toDTO(Route route) {
        RouteDTO dto = new RouteDTO();
        dto.setId(route.getId());
        dto.setName(route.getName());
        dto.setDescription(route.getDescription());

        List<RouteCustomer> rcs = routeCustomerRepository.findByRouteIdOrderByVisitOrderAsc(route.getId());
        List<CustomerDTO> customers = rcs.stream()
                .map(rc -> customerRepository.findById(rc.getCustomerId()))
                .filter(Optional::isPresent)
                .map(c -> customerService.findById(c.get().getId()))
                .collect(Collectors.toList());
        dto.setCustomers(customers);

        return dto;
    }
}
