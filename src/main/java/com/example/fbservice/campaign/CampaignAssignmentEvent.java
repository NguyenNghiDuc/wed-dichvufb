package com.example.fbservice.campaign;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_assignment_events",
       indexes = @Index(name = "idx_campaign_assignment_order", columnList = "campaign_id,created_at"))
public class CampaignAssignmentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;
    @Column(name = "old_assignee", length = 100)
    private String oldAssignee;
    @Column(name = "new_assignee", length = 100)
    private String newAssignee;
    @Column(nullable = false, length = 100)
    private String actor;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected CampaignAssignmentEvent() {}
    public CampaignAssignmentEvent(Long campaignId, String previous, String next, String actor) {
        this.campaignId = campaignId;
        this.oldAssignee = previous;
        this.newAssignee = next;
        this.actor = actor;
    }
    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public String getOldAssignee() { return oldAssignee; }
    public String getNewAssignee() { return newAssignee; }
    public String getActor() { return actor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
