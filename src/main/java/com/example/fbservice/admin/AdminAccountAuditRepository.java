package com.example.fbservice.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccountAuditRepository extends JpaRepository<AdminAccountAudit,Long> {
    List<AdminAccountAudit> findTop100ByOrderByCreatedAtDesc();
}
