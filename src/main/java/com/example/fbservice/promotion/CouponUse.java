package com.example.fbservice.promotion;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="coupon_uses",indexes=@Index(name="idx_coupon_use_owner",columnList="coupon_id,owner_username"))
public class CouponUse {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="coupon_id",nullable=false) private Long couponId;
 @Column(name="order_id",nullable=false,unique=true) private Long orderId;
 @Column(name="owner_username",nullable=false,length=100) private String ownerUsername;
 @Column(name="discount_vnd",nullable=false) private long discountVnd;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 protected CouponUse(){}
 CouponUse(Long coupon,Long order,String owner,long discount,LocalDateTime now){couponId=coupon;orderId=order;ownerUsername=owner;discountVnd=discount;createdAt=now;}
 public Long getOrderId(){return orderId;}public String getOwnerUsername(){return ownerUsername;}public long getDiscountVnd(){return discountVnd;}public LocalDateTime getCreatedAt(){return createdAt;}
}
