package com.wholesale.controller;

import com.wholesale.service.GoogleSheetsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/google-sheets")
public class GoogleSheetsController {

    private final GoogleSheetsService googleSheetsService;

    public GoogleSheetsController(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    /**
     * Get the sync status — is Google Sheets configured, spreadsheet URL, etc.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = googleSheetsService.getStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Manually trigger a full snapshot export from H2 to Google Sheets.
     * Clears the sheet and rewrites all data.
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> exportSnapshot() {
        Map<String, Object> result = new HashMap<>();
        try {
            googleSheetsService.exportFullSnapshot();
            result.put("success", true);
            result.put("message", "Full data snapshot exported to Google Sheets");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Export failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Manually trigger a data load from Google Sheets into H2.
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importFromSheet() {
        Map<String, Object> result = new HashMap<>();
        try {
            googleSheetsService.loadDataIntoH2();
            result.put("success", true);
            result.put("message", "Data imported from Google Sheets into database");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Import failed: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
