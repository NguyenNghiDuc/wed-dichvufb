package com.example.fbservice.campaign;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
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
        // Backwards-compatible endpoint: bounded to 50; use /page for subsequent pages.
        return service.pageMine(principal.getName(), 0, 50).getContent();
    }

    @GetMapping("/page")
    public Page<CampaignOrder> minePage(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) Long orderId,
                                        @RequestParam(required = false) CampaignStatus status,
                                        Principal principal) {
        return service.pageMine(principal.getName(), page, size, orderId, status);
    }

    @GetMapping("/{id}")
    public CampaignOrder mineById(@PathVariable Long id, Principal principal) {
        return service.findMineById(principal.getName(), id);
    }

    /** Only the owning customer can see a campaign's public status timeline. */
    @GetMapping("/{id}/history")
    public ResponseEntity<Page<CampaignHistoryEntry>> history(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        Page<CampaignHistoryEntry> entries = service
                .pageHistoryForOwner(principal.getName(), id, page, size)
                .map(CampaignHistoryEntry::from);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(entries);
    }

    @PostMapping("/{id}/cancel")
    public CampaignOrder cancel(@PathVariable Long id, Principal principal) {
        return service.cancelForOwner(principal.getName(), id);
    }

    @PostMapping
    public ResponseEntity<CampaignOrder> create(@Valid @RequestBody CampaignOrderRequest request,
                                                  Principal principal) {
        CampaignOrder order = service.create(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/v1/campaign-orders/" + order.getId())).body(order);
    }
}
