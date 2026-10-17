package com.example.fbservice.campaign;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_orders", indexes = @Index(name = "idx_campaign_owner", columnList = "owner_username"))
public class CampaignOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_username", nullable = false, length = 100)
    private String ownerUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CampaignServiceType serviceType;

    @Column(nullable = false, length = 512)
    private String targetUrl;

    @Column(nullable = false, length = 1000)
    private String brief;

    @Column(nullable = false)
    private long quotedPriceVnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CampaignStatus status = CampaignStatus.PENDING_REVIEW;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected CampaignOrder() {}

    public CampaignOrder(String ownerUsername, CampaignOrderRequest request) {
        this.ownerUsername = ownerUsername;
        this.platform = request.platform();
        this.serviceType = request.serviceType();
        this.targetUrl = request.targetUrl().trim();
        this.brief = request.brief().trim();
        this.quotedPriceVnd = request.serviceType().getPriceVnd();
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void changeStatus(CampaignStatus next) {
        if (next == null || next == status) throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        boolean valid = switch (status) {
            case PENDING_REVIEW -> next == CampaignStatus.APPROVED || next == CampaignStatus.REJECTED;
            case APPROVED -> next == CampaignStatus.IN_PROGRESS || next == CampaignStatus.REJECTED;
            case IN_PROGRESS -> next == CampaignStatus.COMPLETED;
            case COMPLETED, REJECTED -> false;
        };
        if (!valid) throw new IllegalArgumentException("Không thể chuyển trạng thái từ " + status + " sang " + next);
        this.status = next;
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public SocialPlatform getPlatform() { return platform; }
    public CampaignServiceType getServiceType() { return serviceType; }
    public String getTargetUrl() { return targetUrl; }
    public String getBrief() { return brief; }
    public long getQuotedPriceVnd() { return quotedPriceVnd; }
    public CampaignStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
