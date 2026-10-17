package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:chatIndex27;DB_CLOSE_DELAY=-1")
class ChatConversationIndexIntegrationTest {
    @Autowired ChatService chats;
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username="index-owner")
    void indexFindsUniqueParticipantsOnlyForViewerAndLiteralSearch() throws Exception {
        chats.saveMessage(new ChatMessage("index-owner", "index-somebody", "outgoing"));
        chats.saveMessage(new ChatMessage("index-somebody", "index-owner", "incoming"));
        chats.saveMessage(new ChatMessage("index-owner", "index-team_member", "underscore contact"));
        chats.saveMessage(new ChatMessage("index-private", "index-unrelated", "private"));
        chats.saveMessage(new ChatMessage("index-owner", "index-owner", "self"));
        assertThat(chats.contactNamesFor("index-owner", "", 0).getContent())
            .containsExactly("index-somebody", "index-team_member");
        assertThat(chats.contactNamesFor("index-owner", "_", 0).getContent())
            .containsExactly("index-team_member");
        mvc.perform(get("/chat/ui").param("q", "team_member"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("conversations", contains("index-team_member")))
            .andExpect(content().string(not(containsString("index-private"))));
        mvc.perform(get("/chat/ui").with(user("index-outsider")))
            .andExpect(model().attribute("conversations", empty()));
    }

    @Test @WithMockUser(username="index-paged")
    void indexIsBoundedAndRejectsInvalidSearch() throws Exception {
        for (int i = 0; i < 24; i++)
            chats.saveMessage(new ChatMessage("index-paged", String.format("contact%03d", i), "ping"));
        mvc.perform(get("/chat/ui"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("conversations", hasSize(20)))
            .andExpect(model().attribute("hasNext", true));
        mvc.perform(get("/chat/ui").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("conversations", hasSize(4)));
        mvc.perform(get("/chat/ui").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/chat/ui").param("page", "10001")).andExpect(status().isBadRequest());
        mvc.perform(get("/chat/ui").param("q", "x".repeat(41))).andExpect(status().isBadRequest());
        mvc.perform(get("/chat/ui").param("q", "%")).andExpect(status().isBadRequest());
    }
}
