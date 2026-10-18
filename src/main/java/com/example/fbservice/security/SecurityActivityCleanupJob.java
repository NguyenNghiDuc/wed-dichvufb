package com.example.fbservice.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Optional UTC daily job. Deletion is bounded and idempotent across nodes. */
@Component
@ConditionalOnProperty(prefix="growth.security",name="activity-cleanup-enabled",
                       havingValue="true",matchIfMissing=true)
public class SecurityActivityCleanupJob {
    private final SecurityActivityRetentionService retention;
    public SecurityActivityCleanupJob(SecurityActivityRetentionService retention) {
        this.retention = retention;
    }
    @Scheduled(cron="0 20 3 * * *",zone="UTC")
    public void cleanup() { retention.cleanupNow(); }
}
