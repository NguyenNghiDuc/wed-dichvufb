package com.example.fbservice.security;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityActivityRepository extends JpaRepository<SecurityActivity,Long> {
    List<SecurityActivity> findTop20ByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);
}
