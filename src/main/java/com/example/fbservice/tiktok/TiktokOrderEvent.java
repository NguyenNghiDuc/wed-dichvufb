package com.example.fbservice.tiktok;
import com.example.fbservice.campaign.CampaignStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
@Entity @Table(name="tiktok_order_events") @Getter
public class TiktokOrderEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long orderId;
    @Column(nullable=false,length=100) private String actor;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private CampaignStatus status;
    @Column(nullable=false,length=500) private String description;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    protected TiktokOrderEvent() {}
    public TiktokOrderEvent(Long orderId,String actor,CampaignStatus status,String description){
        this.orderId=orderId;this.actor=actor;this.status=status;this.description=description;
    }
}
