package com.example.fbservice.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_notifications", uniqueConstraints=@UniqueConstraint(name="uk_notification_broadcast_owner",columnNames={"broadcast_id","owner_username"}), indexes = @Index(name = "idx_notifications_owner", columnList = "owner_username,created_at"))
public class AccountNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "owner_username", nullable = false, length = 100) private String ownerUsername;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 500) private String message;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name="broadcast_id") private Long broadcastId;
    private LocalDateTime readAt;

    protected AccountNotification() {}
    public AccountNotification(String owner, String title, String message) {
        this.ownerUsername = owner;
        this.title = title;
        this.message = message;
    }
    public Long getBroadcastId(){return broadcastId;}
    public Long getId() {return id;}
    public String getOwnerUsername(){return ownerUsername;}
    public String getTitle(){return title;}
    public String getMessage(){return message;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getReadAt(){return readAt;}
    public void markRead(){if(readAt==null)readAt=LocalDateTime.now();}
}
