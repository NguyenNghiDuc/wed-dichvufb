package com.example.fbservice.support;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_support_owner", columnList = "owner_username"),
    @Index(name = "idx_support_owner_queue", columnList = "owner_username,status,updated_at,id"),
    @Index(name = "idx_support_staff_queue", columnList = "status,priority,updated_at,id"),
    @Index(name = "idx_support_assignee", columnList = "assignee_username,status,updated_at")
})
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketPriority priority = TicketPriority.NORMAL;
    @Column(name = "assignee_username", length = 100)
    private String assigneeUsername;
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

    /**
     * A new reply must make the ticket visible in the recently-active queue,
     * even when its lifecycle status was already OPEN/ANSWERED.
     * This explicit dirty field avoids relying on @PreUpdate for a no-op status.
     */
    private void recordReplyActivity() {
        LocalDateTime now = LocalDateTime.now();
        updatedAt = now.isAfter(updatedAt) ? now : updatedAt.plusNanos(1_000);
    }

    public void customerReplied() {
        ensureOpen();
        status = TicketStatus.OPEN;
        recordReplyActivity();
    }
    public void staffReplied() {
        ensureOpen();
        status = TicketStatus.ANSWERED;
        recordReplyActivity();
    }
    public void close() {
        ensureOpen();
        status = TicketStatus.CLOSED;
    }
    public void ensureOpen() {
        if (status == TicketStatus.CLOSED) throw new IllegalArgumentException("Yêu cầu đã đóng.");
    }

    public String getAssigneeUsername() { return assigneeUsername; }
    public void claim(String username) {
        ensureOpen();
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Nhân viên không hợp lệ.");
        if (assigneeUsername != null && !assigneeUsername.equals(username))
            throw new IllegalArgumentException("Yêu cầu đang được nhân viên khác xử lý.");
        assigneeUsername = username;
    }
    /** Only the checked, audited ADMIN transfer service may use this setter. */
    public void reassignTo(String nextUsername) {
        ensureOpen();
        this.assigneeUsername = nextUsername;
    }

    public void ensureAssignedStaff(String username) {
        if (assigneeUsername != null && !assigneeUsername.equals(username))
            throw new IllegalArgumentException("Yêu cầu được giao cho nhân viên khác.");
    }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority next) {
        if (next == null) throw new IllegalArgumentException("Mức ưu tiên không hợp lệ.");
        this.priority = next;
    }
    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
