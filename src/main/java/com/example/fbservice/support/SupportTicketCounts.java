package com.example.fbservice.support;

/** Aggregated status counts. Never serialize another customer's ticket list. */
public record SupportTicketCounts(long open, long answered, long closed) {
    public long total() { return open + answered + closed; }
}
