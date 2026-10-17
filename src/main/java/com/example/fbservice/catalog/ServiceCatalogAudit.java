package com.example.fbservice.catalog;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="service_catalog_audit",indexes=@Index(name="idx_service_catalog_audit_date",columnList="created_at"))
public class ServiceCatalogAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=100) private String actor;
    @Column(name="service_code",nullable=false,length=32) private String serviceCode;
    @Column(nullable=false) private long oldPrice;
    @Column(nullable=false) private long newPrice;
    @Column(nullable=false) private boolean oldEnabled;
    @Column(nullable=false) private boolean newEnabled;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();

    protected ServiceCatalogAudit(){}
    public ServiceCatalogAudit(String actor,String serviceCode,
                               long oldPrice,long newPrice,boolean oldEnabled,boolean newEnabled){
        this.actor=actor;this.serviceCode=serviceCode;this.oldPrice=oldPrice;this.newPrice=newPrice;
        this.oldEnabled=oldEnabled;this.newEnabled=newEnabled;
    }
    public Long getId(){return id;}
    public String getActor(){return actor;}
    public String getServiceCode(){return serviceCode;}
    public long getOldPrice(){return oldPrice;}
    public long getNewPrice(){return newPrice;}
    public boolean isOldEnabled(){return oldEnabled;}
    public boolean isNewEnabled(){return newEnabled;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
