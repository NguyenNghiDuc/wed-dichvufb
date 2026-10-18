package com.example.fbservice.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "account_notifications", indexes = @Index(name = "idx_notifications_owner", columnList = "owner_username,created_at"))
public class AccountNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "owner_username", nullable = false, length = 100) private String ownerUsername;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 500) private String message;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime readAt;

    protected AccountNotification() {}
    public AccountNotification(String owner, String title, String message) {
        this.ownerUsername = owner;
        this.title = title;
        this.message = message;
    }
    public Long getId() {return id;}
    public String getOwnerUsername(){return ownerUsername;}
    public String getTitle(){return title;}
    public String getMessage(){return message;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getReadAt(){return readAt;}
    public void markRead(){if(readAt==null)readAt=LocalDateTime.now();}

    /** Display-only classification of legacy notifications; no schema changes. */
    @Transient
    public NotificationCategory getCategory() {
        String text = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (text.contains("hỗ trợ") || text.contains("ticket"))
            return NotificationCategory.SUPPORT;
        if (text.contains("chiến dịch") || text.contains("đơn hàng") || text.contains("đơn #"))
            return NotificationCategory.ORDER;
        if (text.contains("tài khoản") || text.contains("mật khẩu")
                || text.contains("bảo mật") || text.contains("đăng nhập"))
            return NotificationCategory.ACCOUNT;
        return NotificationCategory.OTHER;
    }
    public String getCategoryLabel(){return getCategory().getLabel();}
}
