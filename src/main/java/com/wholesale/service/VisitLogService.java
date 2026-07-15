package com.wholesale.service;

import com.wholesale.dto.VisitLogDTO;
import com.wholesale.model.VisitLog;
import com.wholesale.repository.CustomerRepository;
import com.wholesale.repository.UserRepository;
import com.wholesale.repository.VisitLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class VisitLogService {

    private final VisitLogRepository visitLogRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final GoogleSheetsService googleSheetsService;

    public VisitLogService(VisitLogRepository visitLogRepository,
                           CustomerRepository customerRepository,
                           UserRepository userRepository,
                           GoogleSheetsService googleSheetsService) {
        this.visitLogRepository = visitLogRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.googleSheetsService = googleSheetsService;
    }

    public List<VisitLogDTO> getVisitsByCustomer(Long customerId) {
        return visitLogRepository.findByCustomerIdOrderByVisitedDateDesc(customerId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<VisitLogDTO> getVisitsByDate(LocalDate date) {
        return visitLogRepository.findByVisitedDateOrderByCreatedAtDesc(date).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public VisitLogDTO checkIn(Long customerId, Long userId, String notes, Double lat, Double lon) {
        VisitLog log = new VisitLog();
        log.setCustomerId(customerId);
        log.setUserId(userId);
        log.setVisitedDate(LocalDate.now());
        log.setNotes(notes);
        log.setLatitude(lat);
        log.setLongitude(lon);
        VisitLog saved = visitLogRepository.save(log);

        // Sync to Google Sheets
        try {
            String customerName = "";
            String shopName = "";
            if (customerRepository.findById(customerId).isPresent()) {
                Customer c = customerRepository.findById(customerId).get();
                customerName = c.getName();
                shopName = c.getShopName();
            }
            googleSheetsService.syncCheckIn(customerName, shopName,
                    saved.getVisitedDate(), saved.getNotes(),
                    lat != null ? lat.toString() : null,
                    lon != null ? lon.toString() : null);
        } catch (Exception e) {
            // Sheet sync is best-effort
        }

        return toDTO(saved);
    }

    public boolean isVisitedToday(Long customerId) {
        return visitLogRepository.existsByCustomerIdAndVisitedDate(customerId, LocalDate.now());
    }

    private VisitLogDTO toDTO(VisitLog log) {
        VisitLogDTO dto = new VisitLogDTO();
        dto.setId(log.getId());
        dto.setCustomerId(log.getCustomerId());
        dto.setUserId(log.getUserId());
        dto.setVisitedDate(log.getVisitedDate());
        dto.setNotes(log.getNotes());
        dto.setLatitude(log.getLatitude());
        dto.setLongitude(log.getLongitude());
        dto.setCreatedAt(log.getCreatedAt());

        // Enrich
        customerRepository.findById(log.getCustomerId()).ifPresent(c -> {
            dto.setCustomerName(c.getName());
            dto.setCustomerShopName(c.getShopName());
        });
        userRepository.findById(log.getUserId()).ifPresent(u -> {
            dto.setUserName(u.getDisplayName());
        });

        return dto;
    }
}
