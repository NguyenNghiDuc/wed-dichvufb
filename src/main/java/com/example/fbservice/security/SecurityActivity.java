package com.example.fbservice.security;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Security history deliberately excludes passwords, IP, cookies and session identifiers. */
@Entity
@Table(name="account_security_events",
    indexes=@Index(name="idx_security_account_date",columnList="account_id,created_at"))
public class SecurityActivity {
    public enum Kind { SIGN_IN, SIGN_IN_FAILED, SIGN_IN_LOCKED, PASSWORD_CHANGED, SESSIONS_REVOKED }

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="account_id",nullable=false)
    private Long accountId;
    @Enumerated(EnumType.STRING)
    @Column(name="event_kind",nullable=false,length=32)
    private Kind kind;
    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt;

    protected SecurityActivity() {}

    public SecurityActivity(Long accountId, Kind kind) {
        if (accountId==null || kind==null) throw new IllegalArgumentException("Event owner and type required");
        this.accountId=accountId;
        this.kind=kind;
        this.createdAt=LocalDateTime.now(ZoneOffset.UTC);
    }
    public Long getId(){return id;}
    public Long getAccountId(){return accountId;}
    public Kind getKind(){return kind;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public String getTitle(){
        return switch(kind) {
            case SIGN_IN -> "Đăng nhập thành công";
            case SIGN_IN_FAILED -> "Đăng nhập thất bại";
            case SIGN_IN_LOCKED -> "Đăng nhập bị tạm khóa do quá nhiều lần sai";
            case PASSWORD_CHANGED -> "Đã đổi mật khẩu";
            case SESSIONS_REVOKED -> "Đã đăng xuất tất cả thiết bị";
        };
    }
}
