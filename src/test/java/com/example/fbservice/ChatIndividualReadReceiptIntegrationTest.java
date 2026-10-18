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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w3individualread;DB_CLOSE_DELAY=-1")
class ChatIndividualReadReceiptIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ChatMessageRepository messages;

    @Test
    @WithMockUser(username = "w3-read-recipient")
    void onlyTheRecipientCanAcknowledgeAnIndividualMessageAndReplayIsHarmless() throws Exception {
        ChatMessage mine = messages.saveAndFlush(new ChatMessage(
                "w3-read-sender", "w3-read-recipient", "Incoming private message"));
        ChatMessage other = messages.saveAndFlush(new ChatMessage(
                "w3-read-sender", "w3-read-other", "Another customer's private message"));
        String url = "/chat/messages/" + mine.getId() + "/read";

        mvc.perform(post(url)).andExpect(status().isForbidden()); // CSRF
        mvc.perform(post(url).with(user("w3-read-outsider")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marked").value(0));
        mvc.perform(post(url).with(user("w3-read-sender")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marked").value(0));
        assertThat(messages.findById(mine.getId()).orElseThrow().getReadAt()).isNull();

        mvc.perform(post(url).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marked").value(1));
        mvc.perform(post(url).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marked").value(0));
        mvc.perform(post("/chat/messages/" + other.getId() + "/read").with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marked").value(0));
        mvc.perform(post("/chat/messages/999999999/read").with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marked").value(0));
        mvc.perform(post("/chat/messages/0/read").with(csrf()))
                .andExpect(status().isBadRequest());

        assertThat(messages.findById(mine.getId()).orElseThrow().getReadAt()).isNotNull();
        assertThat(messages.findById(other.getId()).orElseThrow().getReadAt()).isNull();
    }

    @Test
    void unauthenticatedAccountCannotMarkMessageRead() throws Exception {
        mvc.perform(post("/chat/messages/1/read").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
