package com.example.fbservice.campaign;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignEventRepository extends JpaRepository<CampaignEvent, Long> {
    List<CampaignEvent> findByCampaignIdOrderByCreatedAtAsc(Long id);
    Page<CampaignEvent> findByCampaignId(Long campaignId, Pageable pageable);
}
