package com.example.fbservice.controller;

import com.example.fbservice.service.LegacyOrderSummary;
import com.example.fbservice.service.LegacyOrderSummaryService;
import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/legacy-orders")
public class LegacyOrderSummaryApiController {
    private final LegacyOrderSummaryService service;
    public LegacyOrderSummaryApiController(LegacyOrderSummaryService service) {
        this.service = service;
    }
    @GetMapping("/summary")
    public ResponseEntity<LegacyOrderSummary> get(Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.forOwner(principal.getName()));
    }
}
