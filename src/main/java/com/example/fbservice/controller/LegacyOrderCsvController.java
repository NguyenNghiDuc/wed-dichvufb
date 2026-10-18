package com.example.fbservice.controller;

import com.example.fbservice.service.LegacyOrderCsvExportService;
import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class LegacyOrderCsvController {
    private final LegacyOrderCsvExportService exports;
    public LegacyOrderCsvController(LegacyOrderCsvExportService exports) {
        this.exports=exports;
    }

    @GetMapping(value="/export.csv", produces="text/csv;charset=UTF-8")
    public ResponseEntity<String> mine(@RequestParam(required = false) Long orderId,
                                       Principal principal) {
        String csv;
        try {
            csv = exports.exportMine(principal.getName(), orderId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order filter");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"legacy-orders.csv\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv);
    }
}
