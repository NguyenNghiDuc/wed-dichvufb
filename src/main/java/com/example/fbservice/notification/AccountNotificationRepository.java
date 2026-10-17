package com.example.fbservice.notification;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountNotificationRepository extends JpaRepository<AccountNotification,Long> {
    List<AccountNotification> findTop100ByOwnerUsernameOrderByCreatedAtDesc(String username);
    Optional<AccountNotification> findByIdAndOwnerUsername(Long id,String username);
    long countByOwnerUsernameAndReadAtIsNull(String username);
}
