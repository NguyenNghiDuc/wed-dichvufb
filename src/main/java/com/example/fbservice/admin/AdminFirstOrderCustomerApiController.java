package com.example.fbservice.admin;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminFirstOrderCustomerApiController {
    public record FirstOrderCustomers(long customers, String definition) {}
    private final AdminFirstOrderCustomerService service;
    public AdminFirstOrderCustomerApiController(AdminFirstOrderCustomerService service) {
        this.service=service;
    }

    @GetMapping("/api/v1/admin/analytics/first-order-customers")
    public ResponseEntity<FirstOrderCustomers> count() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new FirstOrderCustomers(service.lastThirtyDays(),
                "unique customers whose first campaign order was created within the last 30 days; not registrations"));
    }
}
