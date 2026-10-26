package com.example.fbservice.support;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets", indexes = @Index(name = "idx_support_owner", columnList = "owner_username"))
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_username", nullable = false, length = 100)
    private String ownerUsername;
    @Column(nullable = false, length = 120)
    private String subject;
    @Column(nullable = false, length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected SupportTicket() {}
    SupportTicket(String ownerUsername, String subject, String description) {
        this.ownerUsername = ownerUsername;
        this.subject = subject;
        this.description = description;
    }
    @PreUpdate
    public void touch() { updatedAt = LocalDateTime.now(); }

    public void customerReplied() {
        ensureOpen();
        status = TicketStatus.OPEN;
        touch();
    }
    public void staffReplied() {
        ensureOpen();
        status = TicketStatus.ANSWERED;
        touch();
    }
    public void close() {
        ensureOpen();
        status = TicketStatus.CLOSED;
    }
    public void reopen() {
        if (status != TicketStatus.CLOSED) throw new IllegalArgumentException("Chỉ mở lại yêu cầu đã đóng.");
        status = TicketStatus.OPEN;
    }
    public void ensureOpen() {
        if (status == TicketStatus.CLOSED) throw new IllegalArgumentException("Yêu cầu đã đóng.");
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
