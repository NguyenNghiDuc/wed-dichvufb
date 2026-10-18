package com.example.fbservice.broadcast;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="broadcast_events",indexes=@Index(name="idx_broadcast_event",columnList="broadcast_id,created_at,id"))
public class BroadcastEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="broadcast_id",nullable=false) private Long broadcastId;
 @Column(nullable=false,length=100) private String actor;
 @Column(nullable=false,length=16) private String operation;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 protected BroadcastEvent(){}
 BroadcastEvent(Long id,String actor,String operation,LocalDateTime now){broadcastId=id;this.actor=actor;this.operation=operation;createdAt=now;}
 public String getActor(){return actor;}public String getOperation(){return operation;}public LocalDateTime getCreatedAt(){return createdAt;}
 public String getTitle(){return switch(operation){case "CREATED"->"Tạo bản nháp";case "SENT"->"Gửi thông báo";case "CANCELLED"->"Hủy bản nháp";default->operation;};}
}
