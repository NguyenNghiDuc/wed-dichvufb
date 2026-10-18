package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.repository.ChatMessageRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:w3largechatpages;DB_CLOSE_DELAY=-1")
class ChatLargeThreadPaginationIntegrationTest {
    @Autowired ChatMessageRepository messages;
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "bulk-chat-owner")
    void stablePagingAcross137MessagesDoesNotLeakUnrelatedThreads() throws Exception {
        LocalDateTime sameTimestamp = LocalDateTime.of(2026, 10, 18, 8, 30);
        // Same timestamps force the repository's secondary ORDER BY id DESC
        // to do the work; verify no loss/duplication between pages.
        for (int i = 0; i < 137; i++) {
            ChatMessage message = new ChatMessage(
                    i % 2 == 0 ? "bulk-chat-owner" : "bulk-chat-peer",
                    i % 2 == 0 ? "bulk-chat-peer" : "bulk-chat-owner",
                    "bulk-safe-" + i);
            message.setTimestamp(sameTimestamp);
            messages.save(message);
        }
        messages.save(new ChatMessage("bulk-chat-peer", "bulk-chat-outsider",
                "private-chat-third-party"));
        messages.flush();

        java.util.Set<Long> allIds = new HashSet<>();
        for (int page = 0; page < 3; page++) {
            var response = mvc.perform(get("/chat/thread/bulk-chat-peer")
                            .param("page", Integer.toString(page))
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(137))
                    .andExpect(jsonPath("$.content", hasSize(page < 2 ? 50 : 37)))
                    .andExpect(content().string(not(containsString("private-chat-third-party"))))
                    .andReturn().getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode body =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
            for (com.fasterxml.jackson.databind.JsonNode item : body.path("content")) {
                assertThat(allIds.add(item.path("id").asLong()))
                        .as("duplicate message across pages").isTrue();
            }
        }
        assertThat(allIds).hasSize(137);
        mvc.perform(get("/chat/thread/bulk-chat-peer").param("page", "3").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/chat/thread/bulk-chat-peer")
                        .with(user("bulk-chat-outsider")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(content().string(not(containsString("bulk-safe-"))));
        mvc.perform(get("/chat/thread/bulk-chat-peer").param("size", "51"))
                .andExpect(status().isBadRequest());
    }
}
