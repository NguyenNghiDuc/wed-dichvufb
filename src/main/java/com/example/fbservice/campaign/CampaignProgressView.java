package com.example.fbservice.campaign;

import java.util.List;

/** Read-only internal progress; never a provider performance or fulfillment guarantee. */
public record CampaignProgressView(CampaignStatus status, int percent,
        boolean closedWithoutCompletion, List<Step> steps) {
    public record Step(String label, boolean done, boolean active) {}
}
