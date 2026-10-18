package com.example.fbservice.promotion;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CouponUseRepository extends JpaRepository<CouponUse,Long>{
 long countByCouponIdAndOwnerUsername(Long couponId,String owner);
 Page<CouponUse> findByCouponId(Long couponId,Pageable page);
}
