package com.example.fbservice.security;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface AccountSessionRepository extends JpaRepository<AccountSession,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from AccountSession s where s.sessionHash=:hash")
 Optional<AccountSession> lockByHash(@Param("hash") String hash);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from AccountSession s where s.id=:id and s.userId=:userId")
 Optional<AccountSession> lockOwned(@Param("id") Long id,@Param("userId") Long userId);
 List<AccountSession> findByUserIdAndAuthVersionAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAscIdAsc(Long userId,long version,LocalDateTime now);
 boolean existsByUserId(Long userId);
 boolean existsByUserIdAndIpAddressAndDeviceLabel(Long userId,String ip,String device);
 @Modifying @Query("update AccountSession s set s.revokedAt=:now where s.userId=:userId and s.revokedAt is null")
 int revokeAll(@Param("userId") Long userId,@Param("now") LocalDateTime now);
}
