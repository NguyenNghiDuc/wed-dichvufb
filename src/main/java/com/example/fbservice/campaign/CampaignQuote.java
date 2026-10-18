package com.example.fbservice.campaign;

/** Advisory amount; final order quote is re-read from the server catalog during creation. */
public record CampaignQuote(
    CampaignServiceType serviceType,
    String displayName,
    long priceVnd,
    String currency,
    boolean provisional
) {}
