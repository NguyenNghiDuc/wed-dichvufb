package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignOrder;
import com.example.fbservice.campaign.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/** Exact assignee + status must both be applied in the database query. */
public interface AdminStaffQueueRepository extends Repository<CampaignOrder, Long> {
    Page<CampaignOrder> findByAssigneeUsernameAndStatus(String username,
                                                        CampaignStatus status,
                                                        Pageable pageable);
    Page<CampaignOrder> findByAssigneeUsernameAndId(String username, Long id,
                                                     Pageable pageable);
    Page<CampaignOrder> findByAssigneeUsernameAndIdAndStatus(String username, Long id,
                                                             CampaignStatus status,
                                                             Pageable pageable);

    @Query("select c.status, count(c.id) from CampaignOrder c " +
           "where c.assigneeUsername = :staff group by c.status")
    List<Object[]> countAssignedStatuses(@Param("staff") String staff);
}
