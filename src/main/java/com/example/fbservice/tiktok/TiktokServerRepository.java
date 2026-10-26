package com.example.fbservice.tiktok;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface TiktokServerRepository extends JpaRepository<TiktokServer,Long> {
    List<TiktokServer> findByEnabledTrueOrderByCountryAscNameAscIdAsc();
    List<TiktokServer> findAllByOrderByIdDesc();
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from TiktokServer s where s.id = :id")
    Optional<TiktokServer> lockForOrder(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TiktokServer s where s.id = :id")
    Optional<TiktokServer> lockForUpdate(@Param("id") Long id);
}
