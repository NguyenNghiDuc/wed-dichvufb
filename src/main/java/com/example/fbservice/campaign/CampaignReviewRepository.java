package com.example.fbservice.campaign;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CampaignReviewRepository extends JpaRepository<CampaignReview, Long>, JpaSpecificationExecutor<CampaignReview> {
    Optional<CampaignReview> findByCampaignIdAndOwnerUsername(Long campaignId, String ownerUsername);
    Page<CampaignReview> findByOwnerUsername(String ownerUsername, Pageable pageable);

    interface OwnerRatingCount {
        int getRating();
        long getTotal();
    }

    @Query("select r.rating as rating, count(r.id) as total from CampaignReview r " +
           "where r.ownerUsername = :owner group by r.rating")
    List<OwnerRatingCount> groupedRatingsForOwner(@Param("owner") String owner);
}
