package com.example.fbservice.admin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bounded numeric-only CSV export, generated from the dashboard's exact trend service. */
@RestController
public class AdminOrderTrendCsvController {
    private final AdminOrderTrendService trends;
    public AdminOrderTrendCsvController(AdminOrderTrendService trends) { this.trends=trends; }

    @GetMapping(value="/admin/reports/campaign-trend.csv", produces="text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue="DAY") AdminOrderTrendService.Period period) {
        var trend=trends.find(period);
        StringBuilder csv=new StringBuilder("\uFEFFperiod_start,created_orders,currently_completed_orders\r\n");
        for (var bucket : trend.buckets()) {
            csv.append(bucket.start()).append(',').append(bucket.created()).append(',')
               .append(bucket.completed()).append("\r\n");
        }
        // Enum-derived value only, never an untrusted filename or formula.
        String filename="admin-campaign-trend-"+period.name().toLowerCase(java.util.Locale.ROOT)+".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"")
            .header(HttpHeaders.CACHE_CONTROL,"no-store")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
