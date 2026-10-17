package com.example.fbservice.campaign;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="campaign_events", indexes=@Index(name="idx_campaign_events_order",columnList="campaign_id,created_at"))
public class CampaignEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="campaign_id",nullable=false) private Long campaignId;
    @Enumerated(EnumType.STRING) @Column(length=24) private CampaignStatus oldStatus;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private CampaignStatus newStatus;
    @Column(nullable=false,length=100) private String actor;
    @Column(nullable=false,length=500) private String description;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    protected CampaignEvent(){}
    public CampaignEvent(Long campaignId,CampaignStatus oldStatus,CampaignStatus newStatus,String actor,String description){
        this.campaignId=campaignId;this.oldStatus=oldStatus;this.newStatus=newStatus;this.actor=actor;this.description=description;
    }
    public Long getId(){return id;}
    public Long getCampaignId(){return campaignId;}
    public CampaignStatus getOldStatus(){return oldStatus;}
    public CampaignStatus getNewStatus(){return newStatus;}
    public String getActor(){return actor;}
    public String getDescription(){return description;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
