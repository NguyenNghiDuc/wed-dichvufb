package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.repository.ChatMessageRepository;
import com.example.fbservice.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresChatPaginationIntegrationTest {
    @Autowired ChatService chat;
    @Autowired ChatMessageRepository messages;
    @Autowired JdbcTemplate jdbc;

    @Test
    void productionFlywayIndexesAndOwnerScopedChatQueriesWork() {
        Integer indexCount = jdbc.queryForObject("""
                SELECT count(*) FROM pg_indexes
                WHERE schemaname = current_schema() AND tablename = 'chat_messages'
                  AND indexname IN ('idx_chat_sender_receiver_time', 'idx_chat_receiver_time')
                """, Integer.class);
        assertThat(indexCount).isEqualTo(2);

        messages.save(new ChatMessage("pg-chat-a", "pg-chat-b", "outgoing-pg"));
        messages.save(new ChatMessage("pg-chat-b", "pg-chat-a", "incoming-pg"));
        messages.save(new ChatMessage("pg-chat-b", "pg-chat-c", "not-visible-pg"));

        var result = chat.threadFor("pg-chat-a", "pg-chat-b", 0, 20);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(ChatMessage::getContent)
                .containsExactlyInAnyOrder("outgoing-pg", "incoming-pg");
        assertThat(chat.threadFor("pg-chat-outsider", "pg-chat-b", 0, 20)
                .getTotalElements()).isZero();
        // Validate the grouped contact index/unread query on real PostgreSQL,
        // not only H2; the other customer's thread must not show here.
        assertThat(chat.contactNamesFor("pg-chat-a", "", 0).getContent())
                .containsExactly("pg-chat-b");
        assertThat(chat.unreadCountsForContacts("pg-chat-a",
                java.util.List.of("pg-chat-b"))).containsEntry("pg-chat-b", 1L);
        chat.markThreadRead("pg-chat-a", "pg-chat-b");
        assertThat(chat.unreadCountsForContacts("pg-chat-a",
                java.util.List.of("pg-chat-b"))).containsEntry("pg-chat-b", 0L);
    }
}
