package com.example.fbservice.tiktok;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="tiktok_servers") @Getter @Setter
public class TiktokServer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=100) private String name;
    @Column(nullable=false,length=2) private String country;
    @Column(nullable=false,length=500) private String description;
    @Column(nullable=false) private long unitPriceVnd;
    @Column(nullable=false) private int minimumQuantity;
    @Column(nullable=false) private int maximumQuantity;
    @Column(nullable=false) private boolean enabled;
    @Version private long version;
}
