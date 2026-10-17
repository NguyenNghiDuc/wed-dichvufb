package com.example.fbservice.campaign;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/campaign-orders/{id}/review")
public class CampaignReviewApiController {
    private final CampaignReviewService reviews;
    public CampaignReviewApiController(CampaignReviewService reviews) { this.reviews = reviews; }

    @GetMapping
    public ResponseEntity<CampaignReviewView> get(@PathVariable Long id, Principal principal) {
        CampaignReviewView review = reviews.findForOwner(principal.getName(), id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(review);
    }

    @PostMapping
    public ResponseEntity<CampaignReviewView> submit(@PathVariable Long id,
            @Valid @RequestBody CampaignReviewInput input, Principal principal) {
        CampaignReviewView review = reviews.submit(principal.getName(), id, input);
        return ResponseEntity.created(URI.create("/api/v1/campaign-orders/" + id + "/review"))
            .cacheControl(CacheControl.noStore()).body(review);
    }
}
