package com.example.fbservice.security;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bounded UTC cleanup of audit events, independent of account access flows. */
@Service
public class SecurityActivityRetentionService {
    private final SecurityActivityRepository events;
    private final int retentionDays;

    public SecurityActivityRetentionService(SecurityActivityRepository events,
            @Value("${growth.security.activity-retention-days:90}") int retentionDays) {
        if (retentionDays < 1 || retentionDays > 3650)
            throw new IllegalArgumentException("Security history retention must be between 1 and 3650 days");
        this.events = events;
        this.retentionDays = retentionDays;
    }

    @Transactional
    public int cleanupAt(LocalDateTime utcNow) {
        if (utcNow == null) throw new IllegalArgumentException("UTC time required");
        var ids = events.findExpiredIds(utcNow.minusDays(retentionDays), PageRequest.of(0,500));
        if (ids.isEmpty()) return 0;
        return events.deleteByEventIds(ids);
    }

    public int cleanupNow() {
        return cleanupAt(LocalDateTime.now(ZoneOffset.UTC));
    }
}
