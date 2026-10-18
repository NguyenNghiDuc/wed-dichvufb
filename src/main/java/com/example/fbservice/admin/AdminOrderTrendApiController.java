package com.example.fbservice.admin;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only Admin statistics, no payment or customer profile data. */
@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminOrderTrendApiController {
    private final AdminOrderTrendService service;
    public AdminOrderTrendApiController(AdminOrderTrendService service) { this.service = service; }

    @GetMapping("/campaign-trend")
    public ResponseEntity<AdminOrderTrendService.Trend> get(
            @RequestParam(defaultValue = "DAY") AdminOrderTrendService.Period period) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.find(period));
    }
}
