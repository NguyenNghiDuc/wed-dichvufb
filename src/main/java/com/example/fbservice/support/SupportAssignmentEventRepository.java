package com.example.fbservice.support;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportAssignmentEventRepository extends JpaRepository<SupportAssignmentEvent, Long> {
    List<SupportAssignmentEvent> findTop50ByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);

    /** First successful assignment, derived from existing immutable audit rows. */
    @org.springframework.data.jpa.repository.Query(
        "select min(a.createdAt) from SupportAssignmentEvent a " +
        "where a.ticketId = :ticketId and a.newAssignee is not null")
    java.util.Optional<java.time.LocalDateTime> findFirstAssignedAt(
            @org.springframework.data.repository.query.Param("ticketId") Long ticketId);

}
