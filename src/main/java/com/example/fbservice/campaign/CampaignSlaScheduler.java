package com.example.fbservice.campaign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="growth.sla",name="alerts-enabled",havingValue="true")
public class CampaignSlaScheduler {
    private final CampaignSlaAlertService alerts;
    public CampaignSlaScheduler(CampaignSlaAlertService alerts){this.alerts=alerts;}
    @Scheduled(fixedDelayString="${growth.sla.scan-ms:3600000}")
    public void check(){alerts.scanNow();}
}
