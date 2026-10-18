package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Private, bounded six-month CSV trend without owner input or bulk campaign reads. */
@RestController
public class CustomerCampaignMonthlyCsvController {
    private final CampaignOrderService campaigns;

    public CustomerCampaignMonthlyCsvController(CampaignOrderService campaigns) {
        this.campaigns = campaigns;
    }

    @GetMapping(value = "/campaigns/summary/monthly.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> export(Principal principal) {
        StringBuilder csv = new StringBuilder("\uFEFFmonth,orders\r\n");
        for (CampaignMonthlyCount month : campaigns.monthlyCountsForOwner(principal.getName())) {
            csv.append(CampaignCsvExportService.escape(month.month()))
                .append(',')
                .append(CampaignCsvExportService.escape(month.orders()))
                .append("\r\n");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"my-campaign-monthly.csv\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv.toString());
    }
}
