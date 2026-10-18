package com.example.fbservice.campaign;

/**
 * One owner-scoped calendar-month aggregate; no customer identifiers or
 * individual campaign details are serialized.
 */
public record CampaignMonthlyCount(String month, long orders) {}
