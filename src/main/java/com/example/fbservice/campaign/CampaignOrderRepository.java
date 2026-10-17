package com.example.fbservice.campaign;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CampaignOrderRepository extends JpaRepository<CampaignOrder, Long>,
        JpaSpecificationExecutor<CampaignOrder> {
    List<CampaignOrder> findByOwnerUsernameOrderByCreatedAtDesc(String username);

    // One grouped, owner-scoped read instead of querying each status individually.
    interface OwnerStatusCount {
        CampaignStatus getStatus();
        long getTotal();
    }

    @Query("select c.status as status, count(c.id) as total from CampaignOrder c " +
           "where c.ownerUsername = :username group by c.status")
    List<OwnerStatusCount> countStatusesForOwner(@Param("username") String username);
    Page<CampaignOrder> findByOwnerUsername(String username, Pageable pageable);
    Page<CampaignOrder> findByAssigneeUsername(String username, Pageable pageable);
    Optional<CampaignOrder> findByIdAndOwnerUsername(Long id, String username);
    List<CampaignOrder> findAllByOrderByCreatedAtDesc();
    long countByStatus(CampaignStatus status);
    long countByStatusAndCreatedAtBefore(CampaignStatus status, LocalDateTime cutoff);
    long countByStatusAndUpdatedAtBefore(CampaignStatus status, LocalDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampaignOrder c where c.id = :id and c.ownerUsername = :username")
    Optional<CampaignOrder> lockByIdAndOwner(@Param("id") Long id, @Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampaignOrder c where c.id = :id and c.assigneeUsername = :username")
    Optional<CampaignOrder> lockAssigned(@Param("id") Long id, @Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampaignOrder c where c.id = :id")
    Optional<CampaignOrder> lockById(@Param("id") Long id);
}
