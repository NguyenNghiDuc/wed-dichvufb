package com.example.fbservice.campaign;

import java.time.LocalDateTime;

/**
 * Public customer-facing status timeline entry.
 * Never serialize the internal actor username or staff assignment details.
 */
public record CampaignHistoryEntry(
        Long id,
        CampaignStatus previousStatus,
        CampaignStatus status,
        String description,
        LocalDateTime createdAt
) {
    public static CampaignHistoryEntry from(CampaignEvent event) {
        return new CampaignHistoryEntry(event.getId(), event.getOldStatus(),
                event.getNewStatus(), event.getDescription(), event.getCreatedAt());
    }
}
