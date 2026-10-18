package com.example.fbservice.support;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_messages", indexes = {
    @Index(name = "idx_support_message_ticket", columnList = "ticket_id"),
    @Index(name = "idx_support_msg_timeline", columnList = "ticket_id,created_at,id")
})
public class SupportMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    @Column(nullable = false, length = 100)
    private String author;
    @Column(nullable = false, length = 2000)
    private String body;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected SupportMessage() {}
    SupportMessage(Long ticketId, String author, String body) {
        this.ticketId = ticketId;
        this.author = author;
        this.body = body;
    }
    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public String getAuthor() { return author; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
