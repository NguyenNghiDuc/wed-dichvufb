package com.example.fbservice.broadcast;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="system_broadcasts",indexes=@Index(name="idx_broadcast_created",columnList="created_at,id"))
public class SystemBroadcast {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=120) private String title;
 @Column(nullable=false,length=500) private String message;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private BroadcastAudience audience;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private BroadcastStatus status=BroadcastStatus.DRAFT;
 @Column(name="created_by",nullable=false,length=100) private String createdBy;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="acted_by",length=100) private String actedBy;
 @Column(name="acted_at") private LocalDateTime actedAt;
 @Column(name="recipient_count",nullable=false) private int recipientCount;
 protected SystemBroadcast(){}
 SystemBroadcast(String title,String message,BroadcastAudience audience,String actor,LocalDateTime now){this.title=title;this.message=message;this.audience=audience;createdBy=actor;createdAt=now;}
 void sent(String actor,int count,LocalDateTime now){status=BroadcastStatus.SENT;actedBy=actor;recipientCount=count;actedAt=now;}
 void cancel(String actor,LocalDateTime now){status=BroadcastStatus.CANCELLED;actedBy=actor;actedAt=now;}
 public Long getId(){return id;}public String getTitle(){return title;}public String getMessage(){return message;}
 public BroadcastAudience getAudience(){return audience;}public BroadcastStatus getStatus(){return status;}
 public String getCreatedBy(){return createdBy;}public LocalDateTime getCreatedAt(){return createdAt;}
 public String getActedBy(){return actedBy;}public LocalDateTime getActedAt(){return actedAt;}public int getRecipientCount(){return recipientCount;}
}
