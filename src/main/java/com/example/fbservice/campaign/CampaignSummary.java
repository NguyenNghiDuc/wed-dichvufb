package com.example.fbservice.campaign;

/**
 * Read-only, owner-scoped campaign counts. Values are counts, never monetary amounts.
 */
public record CampaignSummary(
    long total,
    long pendingReview,
    long approved,
    long inProgress,
    long completed,
    long rejected,
    long cancelled
) {}
