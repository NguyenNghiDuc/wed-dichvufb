package com.example.fbservice.campaign;

/** Informational customer advisory: never a contractual service-level promise. */
public record CampaignAttentionView(boolean needsAttention, boolean overdue, String shortLabel,
                                    String message) {}
