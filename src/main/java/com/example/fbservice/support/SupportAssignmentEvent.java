package com.example.fbservice.support;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Immutable audit trail for claims, transfers and unassignments. */
@Entity
@Table(name = "support_assignment_events", indexes = @Index(
    name = "idx_support_assignment_ticket", columnList = "ticket_id,created_at,id"))
public class SupportAssignmentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    @Column(name = "old_assignee", length = 100)
    private String oldAssignee;
    @Column(name = "new_assignee", length = 100)
    private String newAssignee;
    @Column(nullable = false, length = 100)
    private String actor;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected SupportAssignmentEvent() {}
    SupportAssignmentEvent(Long ticketId, String oldAssignee, String newAssignee, String actor) {
        this.ticketId = ticketId;
        this.oldAssignee = oldAssignee;
        this.newAssignee = newAssignee;
        this.actor = actor;
    }
    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public String getOldAssignee() { return oldAssignee; }
    public String getNewAssignee() { return newAssignee; }
    public String getActor() { return actor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
