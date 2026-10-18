package com.example.fbservice.security;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityActivityRepository extends JpaRepository<SecurityActivity,Long> {
    List<SecurityActivity> findTop20ByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);
    List<SecurityActivity> findTop200ByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

    @Query("select e.id from SecurityActivity e where e.createdAt < :cutoff order by e.createdAt asc, e.id asc")
    List<Long> findExpiredIds(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Modifying
    @Query("delete from SecurityActivity e where e.id in :ids")
    int deleteByEventIds(@Param("ids") List<Long> ids);
}
