package com.example.fbservice.support;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {
    Page<SupportTicket> findByOwnerUsername(String username, Pageable pageable);
    long countByStatus(TicketStatus status);
    long countByOwnerUsernameAndStatus(String ownerUsername, TicketStatus status);
    Optional<SupportTicket> findByIdAndOwnerUsername(Long id, String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from SupportTicket t where t.id=:id and t.ownerUsername=:username")
    Optional<SupportTicket> lockForOwner(@Param("id") Long id, @Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from SupportTicket t where t.id=:id")
    Optional<SupportTicket> lockForAdmin(@Param("id") Long id);
}
