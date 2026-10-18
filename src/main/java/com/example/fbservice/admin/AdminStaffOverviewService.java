package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignStatus;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Counters are always for the authenticated STAFF/ADMIN username. */
@Service
public class AdminStaffOverviewService {
    public record Overview(long awaitingApproval, long approved, long inProgress,
                           long completed, long startedLast30Days,
                           long completedLast30Days,
                           long staleApproved, long staleInProgress) {}
    private final AdminStaffQueueRepository queues;
    private final AdminStaffPerformanceService performance;
    private final UserRepository users;

    public AdminStaffOverviewService(AdminStaffQueueRepository queues,
                                     AdminStaffPerformanceService performance,
                                     UserRepository users) {
        this.queues=queues;this.performance=performance;this.users=users;
    }

    @Transactional(readOnly=true)
    public Overview mine(String username) {
        User person=users.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!person.isEnabled() || !(person.getRole().equals("STAFF") ||
                                      person.getRole().equals("ADMIN"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        long[] counts=new long[CampaignStatus.values().length];
        for (Object[] row : queues.countAssignedStatuses(username)) {
            counts[((CampaignStatus)row[0]).ordinal()]=((Number)row[1]).longValue();
        }
        var now = LocalDateTime.now();
        long staleApproved = 0;
        long staleInProgress = 0;
        for (Object[] row : queues.countStaleAssignedStatuses(username,
                CampaignStatus.APPROVED, CampaignStatus.IN_PROGRESS,
                now.minusHours(72), now.minusDays(7))) {
            CampaignStatus status=(CampaignStatus)row[0];
            long count=((Number)row[1]).longValue();
            if (status == CampaignStatus.APPROVED) staleApproved=count;
            if (status == CampaignStatus.IN_PROGRESS) staleInProgress=count;
        }
        var recent=performance.forActor(username);
        return new Overview(counts[CampaignStatus.PENDING_REVIEW.ordinal()],
            counts[CampaignStatus.APPROVED.ordinal()],
            counts[CampaignStatus.IN_PROGRESS.ordinal()],
            counts[CampaignStatus.COMPLETED.ordinal()],
            recent.started(),recent.completed(),staleApproved,staleInProgress);
    }
}
