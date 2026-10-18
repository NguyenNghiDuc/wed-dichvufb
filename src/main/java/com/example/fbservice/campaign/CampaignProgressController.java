package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Owner-authenticated progress is read-only. */
@RestController
@RequestMapping("/api/v1/campaign-orders/{id}/progress")
public class CampaignProgressController {
    private final CampaignOrderService orders;
    private final CampaignProgressService progress;
    public CampaignProgressController(CampaignOrderService orders, CampaignProgressService progress) {
        this.orders=orders;this.progress=progress;
    }
    @GetMapping
    public ResponseEntity<CampaignProgressView> mine(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(progress.view(orders.findMineById(principal.getName(), id)));
    }
}
