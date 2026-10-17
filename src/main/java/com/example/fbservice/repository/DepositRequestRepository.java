package com.example.fbservice.repository;

import com.example.fbservice.model.DepositRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DepositRequestRepository extends JpaRepository<DepositRequest,Long> {
    List<DepositRequest> findByOwnerUsernameOrderByCreatedAtDesc(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepositRequest d where d.id = :id")
    Optional<DepositRequest> findLockedById(@Param("id") Long id);
}
