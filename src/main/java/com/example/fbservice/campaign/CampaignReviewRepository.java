package com.example.fbservice.campaign;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CampaignReviewRepository extends JpaRepository<CampaignReview, Long>, JpaSpecificationExecutor<CampaignReview> {
    Optional<CampaignReview> findByCampaignIdAndOwnerUsername(Long campaignId, String ownerUsername);
    Page<CampaignReview> findByOwnerUsername(String ownerUsername, Pageable pageable);
}
