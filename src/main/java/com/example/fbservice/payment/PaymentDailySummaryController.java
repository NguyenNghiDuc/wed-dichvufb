package com.example.fbservice.payment;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The existing SecurityConfig protects /api/v1/admin/** as ADMIN only.
 * This is read-only aggregate operational data, without event/customer IDs.
 */
@RestController
@RequestMapping("/api/v1/admin/payments/reconciliation")
public class PaymentDailySummaryController {
    private final PaymentDailySummaryService service;

    public PaymentDailySummaryController(PaymentDailySummaryService service) {
        this.service = service;
    }

    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> daily(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.report(days));
    }
}
