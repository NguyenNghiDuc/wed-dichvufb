package com.example.fbservice.campaign;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class CampaignSlaPolicy {
    private final int reviewHours;
    private final int executionHours;
    public CampaignSlaPolicy(@Value("${growth.sla.review-hours:24}") int reviewHours,
                             @Value("${growth.sla.execution-hours:72}") int executionHours){
        if(reviewHours<1 || reviewHours>720 || executionHours<1 || executionHours>720)
            throw new IllegalArgumentException("Deadline must be 1-720 hours");
        this.reviewHours=reviewHours;this.executionHours=executionHours;
    }
    public LocalDateTime pendingDeadline(){return LocalDateTime.now(ZoneOffset.UTC).plusHours(reviewHours);}
    public LocalDateTime executionDeadline(){return LocalDateTime.now(ZoneOffset.UTC).plusHours(executionHours);}
    public int reviewHours(){return reviewHours;}
    public int executionHours(){return executionHours;}
}
