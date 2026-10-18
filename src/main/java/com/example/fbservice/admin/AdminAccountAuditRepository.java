package com.example.fbservice.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminAccountAuditRepository extends JpaRepository<AdminAccountAudit,Long>, JpaSpecificationExecutor<AdminAccountAudit> {
    List<AdminAccountAudit> findTop100ByOrderByCreatedAtDesc();
}
