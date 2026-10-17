package com.example.fbservice.admin;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="admin_account_audit",
       indexes=@Index(name="idx_account_audit_target",columnList="target_user_id,created_at"))
public class AdminAccountAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,length=100)
    private String actor;
    @Column(name="target_user_id",nullable=false)
    private Long targetUserId;
    @Column(nullable=false,length=32)
    private String operation;
    @Column(nullable=false,length=32)
    private String beforeValue;
    @Column(nullable=false,length=32)
    private String afterValue;
    @Column(nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();

    protected AdminAccountAudit() {}

    public AdminAccountAudit(String actor, Long targetUserId,
                             String operation, String beforeValue, String afterValue) {
        this.actor=actor;
        this.targetUserId=targetUserId;
        this.operation=operation;
        this.beforeValue=beforeValue;
        this.afterValue=afterValue;
    }
    public Long getId(){return id;}
    public String getActor(){return actor;}
    public Long getTargetUserId(){return targetUserId;}
    public String getOperation(){return operation;}
    public String getBeforeValue(){return beforeValue;}
    public String getAfterValue(){return afterValue;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
