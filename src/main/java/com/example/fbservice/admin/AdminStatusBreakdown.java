package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignStatus;

/** Read-only values. Quoted amounts are NOT recognized revenue. */
public record AdminStatusBreakdown(CampaignStatus status, long orderCount, long quotedValueVnd) {}
