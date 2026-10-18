package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w3unreadbadge;DB_CLOSE_DELAY=-1")
class ChatUnreadBadgeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ChatMessageRepository messages;

    @Test
    @WithMockUser(username = "w3-unread-recipient")
    void badgeIsPrivateChangesAfterReceiptAndNeverLeaksOtherUserMessages() throws Exception {
        ChatMessage first = messages.saveAndFlush(
                new ChatMessage("w3-unread-sender", "w3-unread-recipient", "Message 1"));
        messages.saveAndFlush(
                new ChatMessage("w3-unread-sender", "w3-unread-recipient", "Message 2"));
        messages.saveAndFlush(
                new ChatMessage("w3-unread-sender", "w3-unread-stranger", "Never shown"));

        mvc.perform(get("/chat/unread/count"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.unread").value(2))
                .andExpect(content().string(not(containsString("w3-unread-stranger"))));
        mvc.perform(get("/chat/ui"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("unreadTotal", 2L))
                .andExpect(content().string(containsString("Bạn có 2 tin nhắn chưa đọc")));

        mvc.perform(post("/chat/messages/" + first.getId() + "/read").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(1));
        mvc.perform(get("/chat/unread/count"))
                .andExpect(jsonPath("$.unread").value(1));
        mvc.perform(get("/chat/unread/count").with(user("w3-unread-stranger")))
                .andExpect(jsonPath("$.unread").value(1));
        mvc.perform(get("/chat/unread/count").with(user("w3-unread-outsider")))
                .andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void anonymousMayNotReadPrivateUnreadCounts() throws Exception {
        mvc.perform(get("/chat/unread/count")).andExpect(status().is3xxRedirection());
    }
}
