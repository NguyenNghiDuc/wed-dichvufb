package com.example.fbservice.campaign;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaign-orders")
public class CampaignOrderApiController {
    private final CampaignOrderService service;

    public CampaignOrderApiController(CampaignOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<CampaignOrder> mine(Principal principal) {
        return service.findMine(principal.getName());
    }

    @GetMapping("/{id}")
    public CampaignOrder mineById(@PathVariable Long id, Principal principal) {
        return service.findMineById(principal.getName(), id);
    }

    @PostMapping
    public ResponseEntity<CampaignOrder> create(@Valid @RequestBody CampaignOrderRequest request,
                                                  Principal principal) {
        CampaignOrder order = service.create(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/v1/campaign-orders/" + order.getId())).body(order);
    }
}
