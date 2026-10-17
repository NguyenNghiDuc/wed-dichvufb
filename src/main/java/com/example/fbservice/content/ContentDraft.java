package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="content_drafts",indexes=@Index(name="idx_draft_owner",columnList="owner_username"))
public class ContentDraft {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="owner_username",nullable=false,length=100) private String ownerUsername;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SocialPlatform platform;
    @Column(nullable=false,length=120) private String title;
    @Column(nullable=false,length=3000) private String body;
    private LocalDateTime scheduledAtUtc;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private DraftStatus status;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();

    protected ContentDraft(){}
    ContentDraft(String owner,SocialPlatform platform,String title,String body,LocalDateTime when){
        this.ownerUsername=owner;this.platform=platform;this.title=title;this.body=body;
        this.scheduledAtUtc=when;this.status=when==null?DraftStatus.DRAFT:DraftStatus.PLANNED;
    }
    /** The saved plan can be revised without publishing content to a platform. */
    void revise(SocialPlatform platform, String title, String body, LocalDateTime when) {
        this.platform = platform;
        this.title = title;
        this.body = body;
        this.scheduledAtUtc = when;
        this.status = when == null ? DraftStatus.DRAFT : DraftStatus.PLANNED;
    }
    public Long getId(){return id;}
    public String getOwnerUsername(){return ownerUsername;}
    public SocialPlatform getPlatform(){return platform;}
    public String getTitle(){return title;}
    public String getBody(){return body;}
    public LocalDateTime getScheduledAtUtc(){return scheduledAtUtc;}
    public DraftStatus getStatus(){return status;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
