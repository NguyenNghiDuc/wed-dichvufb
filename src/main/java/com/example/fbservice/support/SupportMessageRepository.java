package com.example.fbservice.support;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
    Page<SupportMessage> findByTicketId(Long ticketId, Pageable pageable);

    /** Literal substring filter scoped to a single ticket, not SQL LIKE wildcards. */
    @Query("select m from SupportMessage m where m.ticketId = :ticketId " +
           "and locate(:term, lower(m.body)) > 0")
    Page<SupportMessage> searchByBody(@Param("ticketId") Long ticketId,
                                       @Param("term") String term, Pageable pageable);
}
