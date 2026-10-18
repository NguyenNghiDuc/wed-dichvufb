package com.example.fbservice.repository;

import com.example.fbservice.model.ChatMessage;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findBySenderAndReceiver(String sender, String receiver, Pageable pageable);
    Page<ChatMessage> findByReceiver(String receiver, Pageable pageable);

    /** Two-way conversation, filtered by the server-side authenticated principal. */
    @Query("""
        select m from ChatMessage m
        where (m.sender = :viewer and m.receiver = :other)
           or (m.sender = :other and m.receiver = :viewer)
        """)
    Page<ChatMessage> findThread(@Param("viewer") String viewer,
                                 @Param("other") String other,
                                 Pageable pageable);

    /**
     * Alphabetic conversation index. Distinct contacts are extracted only from
     * messages where the signed-in viewer is one of two participants.
     * JPA LOCATE treats search text literally, including percent/underscore.
     */
    @Query(value = """
        select distinct case when m.sender = :viewer then m.receiver else m.sender end as contact
        from ChatMessage m
        where (m.sender = :viewer or m.receiver = :viewer)
          and m.sender <> m.receiver
          and locate(:term, lower(case when m.sender = :viewer then m.receiver else m.sender end)) > 0
        order by contact asc
        """,
        countQuery = """
        select count(distinct case when m.sender = :viewer then m.receiver else m.sender end)
        from ChatMessage m
        where (m.sender = :viewer or m.receiver = :viewer)
          and m.sender <> m.receiver
          and locate(:term, lower(case when m.sender = :viewer then m.receiver else m.sender end)) > 0
        """)
    Page<String> findContactNames(@Param("viewer") String viewer,
                                  @Param("term") String term, Pageable pageable);

    /** One indexed grouped query per displayed contact page, never N+1. */
    interface UnreadContactCount {
        String getContact();
        long getUnreadCount();
    }

    @Query("""
        select m.sender as contact, count(m) as unreadCount
        from ChatMessage m
        where m.receiver = :viewer and m.sender in :contacts and m.readAt is null
        group by m.sender
        """)
    java.util.List<UnreadContactCount> countUnreadByContact(
            @Param("viewer") String viewer,
            @Param("contacts") java.util.List<String> contacts);

    /**
     * Mark one incoming message as read. The receiver predicate prevents a
     * sender or an unrelated account from acknowledging another user's mail.
     * A missing/foreign/already-read ID returns zero without revealing which.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChatMessage m set m.readAt = :readAt " +
           "where m.id = :messageId and m.receiver = :viewer and m.readAt is null")
    int markOneRead(@Param("viewer") String viewer,
                    @Param("messageId") Long messageId,
                    @Param("readAt") LocalDateTime readAt);

    /** Atomic read receipt for messages actually addressed to this recipient. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChatMessage m set m.readAt = :readAt " +
           "where m.receiver = :viewer and m.sender = :other and m.readAt is null")
    int markThreadRead(@Param("viewer") String viewer, @Param("other") String other,
                       @Param("readAt") LocalDateTime readAt);
}
