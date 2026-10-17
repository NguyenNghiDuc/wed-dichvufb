package com.example.fbservice.campaign;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaign-orders")
public class CampaignOrderApiController {
    private final CampaignOrderService service;

    public CampaignOrderApiController(CampaignOrderService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CampaignOrder>> mine(Principal principal) {
        // Backwards-compatible endpoint: bounded to 50; use /page for subsequent pages.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.pageMine(principal.getName(), 0, 50).getContent());
    }

    /** Live server-side quote; a client-provided price is never used. */
    @GetMapping("/quote")
    public ResponseEntity<CampaignQuote> quote(@RequestParam CampaignServiceType serviceType,
                                                Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.quoteForCustomer(principal.getName(), serviceType));
    }

    /** Counts always belong to the authenticated principal; no owner parameter is accepted. */
    @GetMapping("/summary")
    public ResponseEntity<CampaignSummary> summary(Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.summaryForOwner(principal.getName()));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<CampaignOrder>> minePage(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) Long orderId,
                                        @RequestParam(required = false) CampaignStatus status,
                                        @RequestParam(required = false) CampaignServiceType serviceType,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.pageMine(principal.getName(), page, size, orderId, status, serviceType, from, to));
    }

    /** Paginated order progress; does not reveal internal actors or staff usernames. */
    @GetMapping("/{id}/history")
    public ResponseEntity<Page<CampaignHistoryEntry>> history(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.pageHistoryForOwner(principal.getName(), id, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignOrder> mineById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.findMineById(principal.getName(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CampaignOrder> cancel(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.cancelForOwner(principal.getName(), id));
    }

    @PostMapping
    public ResponseEntity<CampaignOrder> create(@Valid @RequestBody CampaignOrderRequest request,
                                                  Principal principal) {
        CampaignOrder order = service.create(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/v1/campaign-orders/" + order.getId()))
            .cacheControl(CacheControl.noStore()).body(order);
    }
}
