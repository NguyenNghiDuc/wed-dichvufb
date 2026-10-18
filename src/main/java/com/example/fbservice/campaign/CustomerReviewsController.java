package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CustomerReviewsController {
    private final CampaignReviewService reviews;
    public CustomerReviewsController(CampaignReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/campaigns/reviews")
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) Integer rating,
                       @RequestParam(required = false) String q,
                       Principal principal, Model model) {
        Page<CampaignReviewView> result = getPage(principal, page, 20, rating, q);
        model.addAttribute("selectedRating", rating);
        model.addAttribute("query", q);
        model.addAttribute("reviews", result.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("hasPrevious", result.hasPrevious());
        model.addAttribute("hasNext", result.hasNext());
        return "customer_reviews";
    }

    @GetMapping("/api/v1/customer/reviews")
    @ResponseBody
    public ResponseEntity<Page<CampaignReviewView>> api(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String q,
            Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(getPage(principal, page, size, rating, q));
    }

    private Page<CampaignReviewView> getPage(Principal principal, int page, int size,
                                                   Integer rating, String q) {
        try {
            return reviews.pageForOwner(principal.getName(), page, size, rating, q);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review paging");
        }
    }
}
