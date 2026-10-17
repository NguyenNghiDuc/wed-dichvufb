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

    public AdminDashboardService(UserRepository users,CampaignOrderRepository campaigns,
                                SupportTicketRepository tickets,DepositRequestRepository deposits,
                                ServiceCatalogService catalog){
        this.users=users;this.campaigns=campaigns;this.tickets=tickets;
        this.deposits=deposits;this.catalog=catalog;
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

    @Transactional(readOnly=true)
    public List<CampaignOrder> latestCampaigns(){
        return campaigns.findAll(PageRequest.of(0,10,Sort.by(
            Sort.Order.desc("createdAt"),Sort.Order.desc("id")))).getContent();
    }
}
