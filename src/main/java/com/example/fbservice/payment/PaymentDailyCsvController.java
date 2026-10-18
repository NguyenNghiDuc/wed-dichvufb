package com.example.fbservice.payment;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bounded ADMIN-only CSV of aggregate internal sandbox receipts.
 * Contains fixed ASCII headings and numeric/date values only: no PII
 * and no attacker-controlled text that could become a spreadsheet formula.
 */
@RestController
@RequestMapping("/api/v1/admin/payments/reconciliation")
public class PaymentDailyCsvController {
    private final PaymentDailySummaryService daily;

    public PaymentDailyCsvController(PaymentDailySummaryService daily) {
        this.daily = daily;
    }

    @GetMapping(value = "/daily.csv", produces = "text/csv")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "7") int days) {
        var report = daily.report(days); // validates the 1..31 day bound
        StringBuilder csv = new StringBuilder("date,internal_receipts,internal_amount_vnd\r\n");
        for (Object value : (List<?>) report.get("daily")) {
            PaymentDailySummaryService.DaySummary row =
                    (PaymentDailySummaryService.DaySummary) value;
            csv.append(row.date()).append(',')
                    .append(row.internalReceiptCount()).append(',')
                    .append(row.internalAmountVnd()).append("\r\n");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"growlab-sandbox-daily.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
