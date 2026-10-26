package com.example.fbservice.security;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="account_security_events",indexes=@Index(name="idx_security_event_user",columnList="user_id,created_at,id"))
public class AccountSecurityEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="user_id",nullable=false) private Long userId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private SecurityEventType type;
 @Column(name="ip_address",nullable=false,length=64) private String ipAddress;
 @Column(name="device_label",nullable=false,length=200) private String deviceLabel;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 protected AccountSecurityEvent(){}
 public AccountSecurityEvent(Long userId,SecurityEventType type,String ip,String device,LocalDateTime now){
  this.userId=userId;this.type=type;ipAddress=ClientMetadata.clean(ip,64);deviceLabel=ClientMetadata.clean(device,200);createdAt=now;
 }
 public Long getId(){return id;} public SecurityEventType getType(){return type;}
 public String getIpAddress(){return ipAddress;} public String getDeviceLabel(){return deviceLabel;}
 public LocalDateTime getCreatedAt(){return createdAt;}
}
