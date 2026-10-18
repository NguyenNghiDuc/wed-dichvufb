package com.example.fbservice.payment;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SecurityConfig already limits /api/v1/admin/** to ADMIN. Never expose raw
 * bank account, customer details, payment signatures or secrets here.
 */
@RestController
@RequestMapping("/api/v1/admin/payments")
public class PaymentReconciliationController {
    private final PaymentReconciliationService service;

    public PaymentReconciliationController(PaymentReconciliationService service) {
        this.service = service;
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.summary());
    }
}
