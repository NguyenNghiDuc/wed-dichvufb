package com.example.fbservice.campaign;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignOrderRepository extends JpaRepository<CampaignOrder, Long> {
    List<CampaignOrder> findByOwnerUsernameOrderByCreatedAtDesc(String username);
    Optional<CampaignOrder> findByIdAndOwnerUsername(Long id, String username);
    List<CampaignOrder> findAllByOrderByCreatedAtDesc();
}
