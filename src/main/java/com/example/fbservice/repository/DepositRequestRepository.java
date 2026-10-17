package com.example.fbservice.repository;

import com.example.fbservice.model.DepositRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {
    // Indexed owner-scoped history; never load an unbounded user's deposits into the UI.
    Page<DepositRequest> findByOwnerUsernameOrderByCreatedAtDescIdDesc(String username, Pageable pageable);

    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepositRequest d where d.id = :id")
    Optional<DepositRequest> findLockedById(@Param("id") Long id);
}
