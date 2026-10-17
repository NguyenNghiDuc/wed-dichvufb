package com.example.fbservice.repository;

import com.example.fbservice.model.DepositRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {
    List<DepositRequest> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);
}
