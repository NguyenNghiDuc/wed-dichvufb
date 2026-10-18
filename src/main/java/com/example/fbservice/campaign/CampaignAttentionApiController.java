package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Read-only owner-scoped order attention signal; never changes status or money. */
@RestController
@RequestMapping("/api/v1/campaign-orders/{id}/attention")
public class CampaignAttentionApiController {
    private final CampaignOrderService orders;
    private final CampaignAttentionService attention;
    public CampaignAttentionApiController(CampaignOrderService orders, CampaignAttentionService attention) {
        this.orders = orders;
        this.attention = attention;
    }
    @GetMapping
    public ResponseEntity<CampaignAttentionView> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(attention.inspect(orders.findMineById(principal.getName(), id)));
    }
}
