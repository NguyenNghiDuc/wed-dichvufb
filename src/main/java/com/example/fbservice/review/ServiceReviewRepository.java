package com.example.fbservice.review;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface ServiceReviewRepository extends JpaRepository<ServiceReview,Long>,JpaSpecificationExecutor<ServiceReview>{
 Optional<ServiceReview> findByCampaignOrderId(Long id);
 Optional<ServiceReview> findByTiktokOrderId(Long id);
}
