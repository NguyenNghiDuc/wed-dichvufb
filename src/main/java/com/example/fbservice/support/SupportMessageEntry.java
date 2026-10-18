package com.example.fbservice.support;

import java.time.LocalDateTime;

/**
 * Minimal public ticket reply DTO. Ownership is checked by the service before
 * mapping any message to this response; raw entities are never exposed.
 */
public record SupportMessageEntry(Long id, String author, String body, LocalDateTime createdAt) {
    public static SupportMessageEntry from(SupportMessage message) {
        return new SupportMessageEntry(message.getId(), message.getAuthor(),
            message.getBody(), message.getCreatedAt());
    }
}
