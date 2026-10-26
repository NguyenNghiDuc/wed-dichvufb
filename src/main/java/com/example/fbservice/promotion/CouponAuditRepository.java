package com.example.fbservice.promotion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CouponAuditRepository extends JpaRepository<CouponAudit,Long>{List<CouponAudit> findTop20ByCouponIdOrderByCreatedAtDescIdDesc(Long id);}
