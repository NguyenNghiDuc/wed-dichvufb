package com.example.fbservice.support;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportAssignmentEventRepository extends JpaRepository<SupportAssignmentEvent, Long> {
    List<SupportAssignmentEvent> findTop50ByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);
}
