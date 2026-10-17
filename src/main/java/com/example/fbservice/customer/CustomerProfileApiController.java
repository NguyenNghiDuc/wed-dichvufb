package com.example.fbservice.customer;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/profile")
public class CustomerProfileApiController {
    private final CustomerProfileService profiles;
    public CustomerProfileApiController(CustomerProfileService profiles) { this.profiles = profiles; }

    @GetMapping
    public ResponseEntity<CustomerProfileView> get(Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(profiles.view(principal.getName()));
    }

    @PutMapping
    public ResponseEntity<CustomerProfileView> update(
            @Valid @RequestBody CustomerProfileRequest request, Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(profiles.update(principal.getName(), request));
    }
}
