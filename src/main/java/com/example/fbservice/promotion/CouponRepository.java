package com.example.fbservice.promotion;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface CouponRepository extends JpaRepository<Coupon,Long>{
 Optional<Coupon> findByCode(String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Coupon c where c.code=:code") Optional<Coupon> lockCode(@Param("code") String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Coupon c where c.id=:id") Optional<Coupon> lockId(@Param("id") Long id);
 Page<Coupon> findByEnabled(boolean enabled,Pageable page);
}
