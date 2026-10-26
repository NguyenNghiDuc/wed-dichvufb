package com.example.fbservice.tiktok;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface TiktokOrderRepository extends JpaRepository<TiktokOrder,Long>,JpaSpecificationExecutor<TiktokOrder> {
    Optional<TiktokOrder> findByIdAndOwnerUsername(Long id,String username);
    List<TiktokOrder> findTop5ByOwnerUsernameOrderByCreatedAtDescIdDesc(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from TiktokOrder o where o.id = :id")
    Optional<TiktokOrder> lockById(@Param("id") Long id);
}
