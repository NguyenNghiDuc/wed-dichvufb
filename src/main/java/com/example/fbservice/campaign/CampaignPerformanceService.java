package com.example.fbservice.campaign;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampaignPerformanceService {
    private final CampaignOrderRepository orders;
    private final UserRepository users;

    public record StaffMetrics(long totalAssigned, long awaitingApproval, long readyToStart,
                               long inProgress, long completed, long overdue) {}

    public CampaignPerformanceService(CampaignOrderRepository orders, UserRepository users){
        this.orders=orders;this.users=users;
    }

    @Transactional(readOnly=true)
    public StaffMetrics metricsForStaff(String username) {
        User member=users.findByUsername(username)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        if(!member.isEnabled()||!(member.getRole().equals("STAFF")||member.getRole().equals("ADMIN")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return new StaffMetrics(
            orders.countByAssigneeUsername(username),
            orders.countByAssigneeUsernameAndStatus(username,CampaignStatus.PENDING_REVIEW),
            orders.countByAssigneeUsernameAndStatus(username,CampaignStatus.APPROVED),
            orders.countByAssigneeUsernameAndStatus(username,CampaignStatus.IN_PROGRESS),
            orders.countByAssigneeUsernameAndStatus(username,CampaignStatus.COMPLETED),
            orders.countByAssigneeUsernameAndStatusInAndDueAtBefore(
                username,CampaignSlaAlertService.ACTIVE,LocalDateTime.now(ZoneOffset.UTC))
        );
    }
}
