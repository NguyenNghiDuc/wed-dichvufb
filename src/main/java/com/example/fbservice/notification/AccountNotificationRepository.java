package com.example.fbservice.notification;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.time.LocalDateTime;
public interface AccountNotificationRepository extends JpaRepository<AccountNotification,Long> {
    List<AccountNotification> findTop100ByOwnerUsernameOrderByCreatedAtDesc(String username);
    Optional<AccountNotification> findByIdAndOwnerUsername(Long id,String username);
    long countByOwnerUsernameAndReadAtIsNull(String username);
    Page<AccountNotification> findByOwnerUsername(String username, Pageable page);
    Page<AccountNotification> findByOwnerUsernameAndReadAtIsNull(String username, Pageable page);
    @Modifying(clearAutomatically=true)
    @Query("update AccountNotification n set n.readAt = :now where n.ownerUsername = :owner and n.readAt is null")
    int markAllRead(@Param("owner") String owner,@Param("now") LocalDateTime now);
}
