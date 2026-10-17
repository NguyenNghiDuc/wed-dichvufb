package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Customer-only CSV export; no user identifier is accepted from the request. */
@RestController
@RequestMapping("/campaigns")
public class CustomerCampaignCsvController {
    private final CampaignCsvExportService exporter;

    public CustomerCampaignCsvController(CampaignCsvExportService exporter) {
        this.exporter = exporter;
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> download(@RequestParam(required = false) Long orderId,
                                           @RequestParam(required = false) CampaignStatus status,
                                           Principal principal) {
        try {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"my-campaigns.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(exporter.exportMine(principal.getName(), orderId, status));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid campaign export filters");
        }
    }
}
