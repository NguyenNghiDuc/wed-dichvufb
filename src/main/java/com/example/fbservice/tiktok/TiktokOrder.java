package com.example.fbservice.tiktok;
import com.example.fbservice.campaign.CampaignStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="tiktok_orders",indexes=@Index(name="idx_tiktok_order_owner",columnList="owner_username,created_at")) @Getter @Setter
public class TiktokOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=100) private String ownerUsername;
    @Column(nullable=false) private Long serverId;
    @Column(nullable=false,length=100) private String serverName;
    @Column(nullable=false,length=2) private String country;
    @Column(nullable=false,length=512) private String targetUrl;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false) private long unitPriceVnd;
    @Column(nullable=false) private long totalPriceVnd;
    @Column(nullable=false,length=1000) private String note;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private CampaignStatus status=CampaignStatus.PENDING_REVIEW;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
}
