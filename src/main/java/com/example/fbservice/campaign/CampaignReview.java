package com.example.fbservice.campaign;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_reviews",
       uniqueConstraints = @UniqueConstraint(name = "uk_campaign_review_campaign", columnNames = "campaign_id"),
       indexes = @Index(name = "idx_campaign_reviews_owner", columnList = "owner_username"))
public class CampaignReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;
    @Column(name = "owner_username", nullable = false, length = 100)
    private String ownerUsername;
    @Column(nullable = false)
    private int rating;
    @Column(nullable = false, length = 500)
    private String feedback;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected CampaignReview() {}
    public CampaignReview(Long campaignId, String ownerUsername, int rating, String feedback) {
        this.campaignId = campaignId;
        this.ownerUsername = ownerUsername;
        this.rating = rating;
        this.feedback = feedback;
    }
    public Long getCampaignId() { return campaignId; }
    public String getOwnerUsername() { return ownerUsername; }
    public int getRating() { return rating; }
    public String getFeedback() { return feedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
