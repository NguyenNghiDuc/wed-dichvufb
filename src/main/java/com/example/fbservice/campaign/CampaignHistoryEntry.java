package com.example.fbservice.campaign;

import java.time.LocalDateTime;

/**
 * Customer-visible history item. Intentionally excludes the event's internal actor.
 */
public record CampaignHistoryEntry(
    CampaignStatus oldStatus,
    CampaignStatus newStatus,
    String description,
    LocalDateTime createdAt
) {}
