package com.example.fbservice.campaign;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/campaign-orders")
public class CampaignCsvExportController {
    private final CampaignCsvExportService exporter;

    public CampaignCsvExportController(CampaignCsvExportService exporter) {
        this.exporter = exporter;
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> download() {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"campaign-orders.csv\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(exporter.exportLatest());
    }
}
