package com.example.fbservice.broadcast;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface SystemBroadcastRepository extends JpaRepository<SystemBroadcast,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from SystemBroadcast b where b.id=:id")
 Optional<SystemBroadcast> lockById(@Param("id") Long id);
 Page<SystemBroadcast> findByStatus(BroadcastStatus status,Pageable page);
}
