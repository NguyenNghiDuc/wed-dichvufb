package com.example.fbservice.admin;

import com.example.fbservice.campaign.*;
import com.example.fbservice.catalog.ServiceCatalogService;
import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.repository.*;
import com.example.fbservice.support.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final UserRepository users;
    private final CampaignOrderRepository campaigns;
    private final SupportTicketRepository tickets;
    private final DepositRequestRepository deposits;
    private final ServiceCatalogService catalog;
    private final AdminAnalyticsRepository analytics;

    public AdminDashboardService(UserRepository users,CampaignOrderRepository campaigns,
                                SupportTicketRepository tickets,DepositRequestRepository deposits,
                                ServiceCatalogService catalog, AdminAnalyticsRepository analytics){
        this.users=users;this.campaigns=campaigns;this.tickets=tickets;
        this.deposits=deposits;this.catalog=catalog;this.analytics=analytics;
    }

    // Provisional review thresholds, not contractual SLAs; owner sign-off required.
    private static final long PENDING_REVIEW_HOURS = 48;
    private static final long IN_PROGRESS_HOURS = 168;

    public record Metrics(long totalUsers,long enabledUsers,long totalCampaigns,
                          long pendingCampaigns,long openTickets,long pendingDepositRequests,
                          int activeServices,long pendingNeedsReview,long progressingNeedsReview) {}

    @Transactional(readOnly=true)
    public Metrics metrics(){
        LocalDateTime now = LocalDateTime.now();
        return new Metrics(users.count(),users.countByEnabledTrue(),campaigns.count(),
            campaigns.countByStatus(CampaignStatus.PENDING_REVIEW),
            tickets.countByStatus(TicketStatus.OPEN),
            deposits.countByStatus("PENDING"),
            catalog.available().size(),
            campaigns.countByStatusAndCreatedAtBefore(CampaignStatus.PENDING_REVIEW,
                now.minusHours(PENDING_REVIEW_HOURS)),
            campaigns.countByStatusAndUpdatedAtBefore(CampaignStatus.IN_PROGRESS,
                now.minusHours(IN_PROGRESS_HOURS)));
    }

    public record OperationsSummary(List<AdminStatusBreakdown> statuses,
                                    List<AdminStaffLoad> staffLoad,
                                    long pipelineQuotedValueVnd) {}

    @Transactional(readOnly=true)
    public OperationsSummary operations() {
        List<AdminStatusBreakdown> breakdown = analytics.groupByStatus();
        // These are indicative quotes on unclosed orders; NEVER real revenue.
        long pipeline = breakdown.stream()
            .filter(r -> r.status() == CampaignStatus.PENDING_REVIEW
                      || r.status() == CampaignStatus.APPROVED
                      || r.status() == CampaignStatus.IN_PROGRESS)
            .mapToLong(AdminStatusBreakdown::quotedValueVnd).sum();
        List<AdminStaffLoad> staff = analytics.activeStaffLoad(
            List.of(CampaignStatus.APPROVED, CampaignStatus.IN_PROGRESS), PageRequest.of(0, 8));
        return new OperationsSummary(breakdown, staff, pipeline);
    }

    @Transactional(readOnly=true)
    public List<CampaignOrder> latestCampaigns(){
        return campaigns.findAll(PageRequest.of(0,10,Sort.by(
            Sort.Order.desc("createdAt"),Sort.Order.desc("id")))).getContent();
    }
}
