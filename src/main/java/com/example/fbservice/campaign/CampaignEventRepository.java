package com.example.fbservice.campaign;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignEventRepository extends JpaRepository<CampaignEvent,Long> {
    List<CampaignEvent> findByCampaignIdOrderByCreatedAtAsc(Long id);

    // Bounded read for history API and campaign details. Use a stable ID tiebreaker.
    Page<CampaignEvent> findByCampaignId(Long campaignId, Pageable pageable);
}
