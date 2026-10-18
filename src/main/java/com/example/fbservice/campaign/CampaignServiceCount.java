package com.example.fbservice.campaign;

/** Owner-only aggregated order count per service; no individual account fields. */
public record CampaignServiceCount(CampaignServiceType serviceType, String displayName, long count) {}
