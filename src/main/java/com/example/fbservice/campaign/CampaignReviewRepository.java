package com.example.fbservice.campaign;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignReviewRepository extends JpaRepository<CampaignReview, Long> {
    Optional<CampaignReview> findByCampaignIdAndOwnerUsername(Long campaignId, String ownerUsername);
}
