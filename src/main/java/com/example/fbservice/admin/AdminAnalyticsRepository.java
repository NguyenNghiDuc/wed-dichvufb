package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignOrder;
import com.example.fbservice.campaign.CampaignStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only Admin projections, independent from customer/order mutation repositories. */
public interface AdminAnalyticsRepository extends Repository<CampaignOrder, Long> {
    @Query("""
        select new com.example.fbservice.admin.AdminStatusBreakdown(
            c.status, count(c.id), sum(c.quotedPriceVnd))
        from CampaignOrder c group by c.status order by c.status
        """)
    List<AdminStatusBreakdown> groupByStatus();

    @Query("""
        select new com.example.fbservice.admin.AdminStaffLoad(
            c.assigneeUsername, count(c.id))
        from CampaignOrder c
        where c.assigneeUsername is not null and c.status in :statuses
        group by c.assigneeUsername
        order by count(c.id) desc, c.assigneeUsername asc
        """)
    List<AdminStaffLoad> activeStaffLoad(@Param("statuses") List<CampaignStatus> statuses,
                                         Pageable pageable);
}
