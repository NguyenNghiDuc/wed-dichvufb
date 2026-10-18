package com.example.fbservice.support;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Read-only timeline derived from the ticket itself. The timestamps are
 * stored in server-local time; lastActivityAt is NOT an SLA or bank event.
 * No migration or background scheduler is required.
 */
public record SupportTicketProgress(
        TicketStatus status, TicketPriority priority, boolean assigned,
        LocalDateTime createdAt, LocalDateTime lastActivityAt,
        long ageMinutes, long idleMinutes) {

    public static SupportTicketProgress from(SupportTicket ticket, LocalDateTime now) {
        long age = Math.max(0, Duration.between(ticket.getCreatedAt(), now).toMinutes());
        long idle = Math.max(0, Duration.between(ticket.getUpdatedAt(), now).toMinutes());
        return new SupportTicketProgress(ticket.getStatus(), ticket.getPriority(),
                ticket.getAssigneeUsername() != null, ticket.getCreatedAt(),
                ticket.getUpdatedAt(), age, idle);
    }
}
