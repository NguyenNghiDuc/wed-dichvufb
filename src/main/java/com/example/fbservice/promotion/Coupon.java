package com.example.fbservice.promotion;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="coupons",indexes=@Index(name="idx_coupon_created",columnList="created_at,id"))
public class Coupon {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=32) private String code;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private CouponType type;
 @Column(nullable=false) private long amount;
 @Column(name="maximum_discount_vnd",nullable=false) private long maximumDiscountVnd;
 @Column(name="minimum_order_vnd",nullable=false) private long minimumOrderVnd;
 @Column(name="maximum_uses",nullable=false) private int maximumUses;
 @Column(name="per_user_limit",nullable=false) private int perUserLimit;
 @Column(name="used_count",nullable=false) private int usedCount;
 @Column(name="starts_at",nullable=false) private LocalDateTime startsAt;
 @Column(name="ends_at",nullable=false) private LocalDateTime endsAt;
 @Column(nullable=false) private boolean enabled=true;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 protected Coupon(){}
 Coupon(String code,CouponType type,long amount,long cap,long minimum,int max,int perUser,LocalDateTime start,LocalDateTime end,LocalDateTime now){this.code=code;this.type=type;this.amount=amount;maximumDiscountVnd=cap;minimumOrderVnd=minimum;maximumUses=max;perUserLimit=perUser;startsAt=start;endsAt=end;createdAt=now;}
 void consume(){usedCount++;}void setEnabled(boolean value){enabled=value;}
 public Long getId(){return id;}public String getCode(){return code;}public CouponType getType(){return type;}public long getAmount(){return amount;}
 public long getMaximumDiscountVnd(){return maximumDiscountVnd;}public long getMinimumOrderVnd(){return minimumOrderVnd;}
 public int getMaximumUses(){return maximumUses;}public int getPerUserLimit(){return perUserLimit;}public int getUsedCount(){return usedCount;}
 public LocalDateTime getStartsAt(){return startsAt;}public LocalDateTime getEndsAt(){return endsAt;}public boolean isEnabled(){return enabled;}public LocalDateTime getCreatedAt(){return createdAt;}
}
