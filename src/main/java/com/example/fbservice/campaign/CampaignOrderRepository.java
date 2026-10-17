package com.example.fbservice.campaign;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CampaignOrderRepository extends JpaRepository<CampaignOrder, Long> {
    List<CampaignOrder> findByOwnerUsernameOrderByCreatedAtDesc(String username);
    Page<CampaignOrder> findByOwnerUsername(String username, Pageable pageable);
    Optional<CampaignOrder> findByIdAndOwnerUsername(Long id, String username);
    List<CampaignOrder> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampaignOrder c where c.id = :id and c.ownerUsername = :username")
    Optional<CampaignOrder> lockByIdAndOwner(@Param("id") Long id, @Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampaignOrder c where c.id = :id")
    Optional<CampaignOrder> lockById(@Param("id") Long id);
}
