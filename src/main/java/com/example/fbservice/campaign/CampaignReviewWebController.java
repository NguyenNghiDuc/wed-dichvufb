package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class CampaignReviewWebController {
    private final CampaignReviewService reviews;
    public CampaignReviewWebController(CampaignReviewService reviews) { this.reviews = reviews; }

    @PostMapping("/campaigns/{id}/review")
    public String submit(@PathVariable Long id, @RequestParam(required = false) Integer rating,
                         @RequestParam(required = false) String feedback, Principal principal) {
        try {
            reviews.submit(principal.getName(), id, new CampaignReviewInput(rating, feedback));
            return "redirect:/campaigns/" + id + "?reviewSubmitted=true";
        } catch (IllegalArgumentException exception) {
            return "redirect:/campaigns/" + id + "?reviewError=true";
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            if (exception.getStatusCode().value() != 409) throw exception;
            return "redirect:/campaigns/" + id + "?reviewError=true";
        }
    }
}
