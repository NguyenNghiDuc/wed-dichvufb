package com.example.fbservice.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountNotificationRepository extends JpaRepository<AccountNotification, Long> {
    // Retained for existing consumers while the inbox uses bounded page queries.
    List<AccountNotification> findTop100ByOwnerUsernameOrderByCreatedAtDesc(String username);
    Optional<AccountNotification> findByIdAndOwnerUsername(Long id, String username);
    long countByOwnerUsernameAndReadAtIsNull(String username);

    Page<AccountNotification> findByOwnerUsername(String username, Pageable pageable);
    Page<AccountNotification> findByOwnerUsernameAndReadAtIsNull(String username, Pageable pageable);

    /**
     * Atomic, owner-scoped, repeat-safe bulk update. Never loads another customer's
     * notifications in application memory and does not overwrite previous read times.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AccountNotification n set n.readAt = :readAt " +
           "where n.ownerUsername = :username and n.readAt is null")
    int markAllUnreadAsRead(@Param("username") String username,
                            @Param("readAt") LocalDateTime readAt);
}
