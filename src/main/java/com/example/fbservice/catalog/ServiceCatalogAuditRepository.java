package com.example.fbservice.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ServiceCatalogAuditRepository extends JpaRepository<ServiceCatalogAudit,Long>{
    List<ServiceCatalogAudit> findTop100ByOrderByCreatedAtDesc();
}
