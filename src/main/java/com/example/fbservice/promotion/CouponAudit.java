package com.example.fbservice.promotion;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="coupon_audit",indexes=@Index(name="idx_coupon_audit",columnList="coupon_id,created_at,id"))
public class CouponAudit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="coupon_id",nullable=false) private Long couponId;
 @Column(nullable=false,length=100) private String actor;
 @Column(nullable=false,length=16) private String operation;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 protected CouponAudit(){}
 CouponAudit(Long coupon,String actor,String operation,LocalDateTime now){couponId=coupon;this.actor=actor;this.operation=operation;createdAt=now;}
 public String getActor(){return actor;}public String getOperation(){return switch(operation){case "CREATED"->"Tạo mã";case "ENABLED"->"Bật mã";case "DISABLED"->"Tắt mã";default->operation;};}public LocalDateTime getCreatedAt(){return createdAt;}
}
