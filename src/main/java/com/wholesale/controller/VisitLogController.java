package com.wholesale.controller;

import com.wholesale.dto.VisitLogDTO;
import com.wholesale.service.VisitLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visits")
public class VisitLogController {

    private final VisitLogService visitLogService;

    public VisitLogController(VisitLogService visitLogService) {
        this.visitLogService = visitLogService;
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<VisitLogDTO>> getVisitsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(visitLogService.getVisitsByCustomer(customerId));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<VisitLogDTO>> getVisitsByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        return ResponseEntity.ok(visitLogService.getVisitsByDate(localDate));
    }

    @GetMapping("/today")
    public ResponseEntity<List<VisitLogDTO>> getTodayVisits() {
        return ResponseEntity.ok(visitLogService.getVisitsByDate(LocalDate.now()));
    }

    @PostMapping("/checkin")
    public ResponseEntity<VisitLogDTO> checkIn(@RequestBody Map<String, Object> body,
                                                @RequestHeader("X-User-Id") Long userId) {
        Long customerId = Long.valueOf(body.get("customerId").toString());
        String notes = (String) body.get("notes");
        Double lat = body.containsKey("latitude") ? Double.valueOf(body.get("latitude").toString()) : null;
        Double lon = body.containsKey("longitude") ? Double.valueOf(body.get("longitude").toString()) : null;
        return ResponseEntity.ok(visitLogService.checkIn(customerId, userId, notes, lat, lon));
    }

    @GetMapping("/check-today/{customerId}")
    public ResponseEntity<Map<String, Boolean>> checkVisitedToday(@PathVariable Long customerId) {
        return ResponseEntity.ok(Map.of("visited", visitLogService.isVisitedToday(customerId)));
    }
}
