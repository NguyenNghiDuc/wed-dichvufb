package com.example.fbservice.campaign;

import java.time.LocalDateTime;

/** Public-to-owner only. No usernames, payment details or staff metadata. */
public record CampaignReviewView(long campaignId, int rating, String feedback, LocalDateTime createdAt) {
    public static CampaignReviewView from(CampaignReview review) {
        return new CampaignReviewView(review.getCampaignId(),
            review.getRating(), review.getFeedback(), review.getCreatedAt());
    }
}
