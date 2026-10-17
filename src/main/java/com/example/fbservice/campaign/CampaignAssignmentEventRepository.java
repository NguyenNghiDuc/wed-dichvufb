package com.example.fbservice.campaign;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignAssignmentEventRepository extends JpaRepository<CampaignAssignmentEvent,Long> {
    List<CampaignAssignmentEvent> findByCampaignIdOrderByCreatedAtAsc(Long campaignId);
}
