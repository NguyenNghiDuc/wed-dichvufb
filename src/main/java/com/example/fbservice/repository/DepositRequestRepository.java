package com.example.fbservice.repository;

import com.example.fbservice.model.DepositRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DepositRequestRepository extends JpaRepository<DepositRequest,Long>, JpaSpecificationExecutor<DepositRequest> {
    Optional<DepositRequest> findByIdAndOwnerUsername(Long id,String username);
    List<DepositRequest> findByOwnerUsernameOrderByCreatedAtDesc(String username);
    long countByStatus(String status);
    long countByStatusAndCreatedAtBefore(String status,LocalDateTime before);
    @Query("select count(d) from DepositRequest d where d.status='APPROVED' " +
           "and not exists (select c.id from WalletCredit c where c.depositRequestId=d.id)")
    long countApprovedWithoutCredit();
    @Query("select d from DepositRequest d where d.status='APPROVED' " +
           "and not exists (select c.id from WalletCredit c where c.depositRequestId=d.id) " +
           "order by d.createdAt desc, d.id desc")
    Page<DepositRequest> findApprovedWithoutCredit(Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepositRequest d where d.id = :id")
    Optional<DepositRequest> findLockedById(@Param("id") Long id);
}
