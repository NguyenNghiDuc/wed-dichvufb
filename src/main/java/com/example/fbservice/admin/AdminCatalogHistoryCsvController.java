package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignServiceType;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only current-page export of catalog audit, not a full immutable audit archive. */
@RestController
public class AdminCatalogHistoryCsvController {
    private final AdminCatalogHistoryService history;

    public AdminCatalogHistoryCsvController(AdminCatalogHistoryService history) {
        this.history = history;
    }

    @GetMapping(value = "/admin/catalog/history/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) CampaignServiceType type,
            @RequestParam(required = false) String actor) {
        var result = history.page(page, type, actor);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("timestamp,actor,service_code,old_price_vnd,new_price_vnd,was_enabled,is_enabled\r\n");
        for (var event : result.events()) {
            csv.append(AdminActivityCsvController.cell(event.getCreatedAt().toString())).append(',')
               .append(AdminActivityCsvController.cell(event.getActor())).append(',')
               .append(AdminActivityCsvController.cell(event.getServiceCode())).append(',')
               .append(event.getOldPrice()).append(',')
               .append(event.getNewPrice()).append(',')
               .append(event.isOldEnabled()).append(',')
               .append(event.isNewEnabled()).append("\r\n");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"growlab-catalog-audit-page.csv\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
