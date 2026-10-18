package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.repository.ChatMessageRepository;
import com.example.fbservice.service.ChatService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
        "spring.datasource.username=growlab",
        "spring.datasource.password=testpass",
        "server.servlet.session.cookie.secure=false"
})
class PostgresChatIndividualReadConcurrencyTest {
    @Autowired ChatService chat;
    @Autowired ChatMessageRepository messages;
    @Autowired JdbcTemplate jdbc;

    @Test
    void simultaneousAcknowledgementsUpdateOnlyOneIncomingMessageExactlyOnce() throws Exception {
        ChatMessage mine = messages.saveAndFlush(new ChatMessage(
                "pg-w3-sender", "pg-w3-recipient", "Private PG message"));
        ChatMessage unrelated = messages.saveAndFlush(new ChatMessage(
                "pg-w3-sender", "pg-w3-other", "Not this recipient's message"));

        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> {
                start.await();
                return chat.markMessageRead("pg-w3-recipient", mine.getId());
            });
            var b = executor.submit(() -> {
                start.await();
                return chat.markMessageRead("pg-w3-recipient", mine.getId());
            });
            start.countDown();
            assertThat(a.get(15, TimeUnit.SECONDS) + b.get(15, TimeUnit.SECONDS))
                    .isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        assertThat(chat.markMessageRead("pg-w3-sender", mine.getId())).isZero();
        assertThat(chat.markMessageRead("pg-w3-other", mine.getId())).isZero();
        assertThat(chat.markMessageRead("pg-w3-recipient", unrelated.getId())).isZero();
        assertThat(messages.findById(mine.getId()).orElseThrow().getReadAt()).isNotNull();
        assertThat(messages.findById(unrelated.getId()).orElseThrow().getReadAt()).isNull();

        Integer index = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = current_schema()
                  AND tablename = 'chat_messages'
                  AND indexname = 'idx_chat_received_unread'
                """, Integer.class);
        assertThat(index).isEqualTo(1);
    }
}
