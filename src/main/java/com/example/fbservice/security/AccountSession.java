package com.example.fbservice.security;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="account_sessions",indexes=@Index(name="idx_account_session_user",columnList="user_id,revoked_at,expires_at"))
public class AccountSession {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(name="session_hash",nullable=false,unique=true,length=64) private String sessionHash;
 @Column(name="auth_version",nullable=false) private long authVersion;
 @Column(name="ip_address",nullable=false,length=64) private String ipAddress;
 @Column(name="device_label",nullable=false,length=200) private String deviceLabel;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="last_seen_at",nullable=false) private LocalDateTime lastSeenAt;
 @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
 @Column(name="revoked_at") private LocalDateTime revokedAt;
 protected AccountSession(){}
 AccountSession(Long userId,String hash,long version,String ip,String device,LocalDateTime now,int timeout){
  this.userId=userId;sessionHash=hash;authVersion=version;ipAddress=ip;deviceLabel=device;createdAt=now;touch(now,timeout);
 }
 void touch(LocalDateTime now,int timeout){lastSeenAt=now;expiresAt=now.plusSeconds(Math.max(1,timeout));}
 void revoke(LocalDateTime now){if(revokedAt==null)revokedAt=now;}
 boolean valid(Long owner,long version,LocalDateTime now){return userId.equals(owner)&&authVersion==version&&revokedAt==null&&expiresAt.isAfter(now);}
 boolean matches(String hash){return sessionHash.equals(hash);}
 public Long getId(){return id;} public String getIpAddress(){return ipAddress;}
 public String getDeviceLabel(){return deviceLabel;} public LocalDateTime getCreatedAt(){return createdAt;}
 public LocalDateTime getLastSeenAt(){return lastSeenAt;} public LocalDateTime getExpiresAt(){return expiresAt;}
}
