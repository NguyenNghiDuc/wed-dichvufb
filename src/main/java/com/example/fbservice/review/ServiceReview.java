package com.example.fbservice.review;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="service_reviews",uniqueConstraints={@UniqueConstraint(name="uk_review_campaign",columnNames="campaign_order_id"),@UniqueConstraint(name="uk_review_tiktok",columnNames="tiktok_order_id")},indexes=@Index(name="idx_review_created",columnList="created_at,id"))
public class ServiceReview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="campaign_order_id") private Long campaignOrderId;
 @Column(name="tiktok_order_id") private Long tiktokOrderId;
 @Column(name="owner_username",nullable=false,length=100) private String ownerUsername;
 @Column(nullable=false) private int rating;
 @Column(nullable=false,length=1000) private String comment;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 protected ServiceReview(){}
 ServiceReview(ReviewKind kind,Long orderId,String owner,int rating,String comment,LocalDateTime now){
  if(kind==ReviewKind.CAMPAIGN)campaignOrderId=orderId;else tiktokOrderId=orderId;
  ownerUsername=owner;this.rating=rating;this.comment=comment;createdAt=now;
 }
 public Long getId(){return id;} public String getOwnerUsername(){return ownerUsername;}
 public int getRating(){return rating;} public String getComment(){return comment;}
 public LocalDateTime getCreatedAt(){return createdAt;}
 public ReviewKind getKind(){return campaignOrderId!=null?ReviewKind.CAMPAIGN:ReviewKind.TIKTOK;}
 public Long getOrderId(){return campaignOrderId!=null?campaignOrderId:tiktokOrderId;}
}
