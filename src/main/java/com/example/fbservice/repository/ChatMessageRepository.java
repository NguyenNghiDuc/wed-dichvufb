package com.example.fbservice.repository;

import com.example.fbservice.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Keep the legacy directions independently scoped and bounded by service-supplied PageRequest.
    Page<ChatMessage> findBySenderAndReceiver(String sender, String receiver, Pageable pageable);
    Page<ChatMessage> findByReceiver(String receiver, Pageable pageable);

    // Both directions of exactly one conversation; user identity is supplied by Principal,
    // never accepted from a request query parameter.
    @Query("""
        select m from ChatMessage m
        where (m.sender = :viewer and m.receiver = :other)
           or (m.sender = :other and m.receiver = :viewer)
        """)
    /** Atomic owner-scoped read receipt: only the real recipient may acknowledge their mail. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChatMessage m set m.readAt = :readAt " +
           "where m.receiver = :viewer and m.sender = :other and m.readAt is null")
    int markThreadRead(@Param("viewer") String viewer, @Param("other") String other,
                       @Param("readAt") LocalDateTime readAt);

    Page<ChatMessage> findThread(@Param("viewer") String viewer,
                                 @Param("other") String other,
                                 Pageable pageable);
}
