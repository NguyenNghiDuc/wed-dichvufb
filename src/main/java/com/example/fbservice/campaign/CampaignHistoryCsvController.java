package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/campaigns")
public class CampaignHistoryCsvController {
    private final CampaignHistoryCsvExportService exporter;
    public CampaignHistoryCsvController(CampaignHistoryCsvExportService exporter) {
        this.exporter = exporter;
    }

    @GetMapping(value = "/{id}/history.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> download(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"campaign-history.csv\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(exporter.export(principal.getName(), id));
    }
}
