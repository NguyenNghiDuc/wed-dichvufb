package com.example.fbservice.catalog;

import com.example.fbservice.campaign.CampaignServiceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="service_offerings")
public class ServiceOffering {
    @Id @Column(length=32)
    private String code;
    @Column(nullable=false,length=120)
    private String title;
    @Column(name="price_vnd",nullable=false)
    private long priceVnd;
    @Column(nullable=false)
    private boolean enabled;
    @Version
    private long version;
    @Column(nullable=false)
    private LocalDateTime updatedAt=LocalDateTime.now();

    protected ServiceOffering(){}

    public ServiceOffering(CampaignServiceType type){
        this.code=type.name();
        this.title=type.getDisplayName();
        this.priceVnd=type.getPriceVnd();
        this.enabled=true;
    }

    public void update(long priceVnd,boolean enabled){
        if(priceVnd<10000||priceVnd>50000000)
            throw new IllegalArgumentException("Giá dịch vụ cần từ 10.000 đến 50.000.000 VND.");
        this.priceVnd=priceVnd;
        this.enabled=enabled;
    }
    @PreUpdate public void touch(){updatedAt=LocalDateTime.now();}

    public String getCode(){return code;}
    public String getTitle(){return title;}
    public long getPriceVnd(){return priceVnd;}
    public boolean isEnabled(){return enabled;}
    public long getVersion(){return version;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}
