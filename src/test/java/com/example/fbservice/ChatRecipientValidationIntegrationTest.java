package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.ChatMessageRepository;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w3recipientvalidation;DB_CLOSE_DELAY=-1")
class ChatRecipientValidationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ChatMessageRepository messages;

    private void createRecipient(String name, boolean enabled) {
        User recipient = new User();
        recipient.setUsername(name);
        recipient.setRole("USER");
        recipient.setEnabled(enabled);
        recipient.setPassword("$2a$10$testOnlyRecipientPasswordHash");
        users.save(recipient);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder send(
            String receiver, String content) {
        try {
            String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                    java.util.Map.of("receiver", receiver, "content", content,
                            "sender", "forged-other-account", "id", 999999));
            return post("/chat/send").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    @WithMockUser(username = "w3-sender-account")
    void rejectsSelfNonexistentDisabledAndMalformedRecipientsBeforeSaving() throws Exception {
        createRecipient("w3-disabled-recipient", false);
        long before = messages.count();
        mvc.perform(send("w3-sender-account", "Hello")).andExpect(status().isBadRequest());
        mvc.perform(send("a", "Hello")).andExpect(status().isBadRequest());
        mvc.perform(send("<img src=x onerror=alert(1)>", "Hello"))
                .andExpect(status().isBadRequest());
        mvc.perform(send("w3-unknown-recipient", "Hello")).andExpect(status().isNotFound());
        mvc.perform(send("w3-disabled-recipient", "Hello")).andExpect(status().isNotFound());
        mvc.perform(send("w3-disabled-recipient", " ")).andExpect(status().isBadRequest());
        assertThat(messages.count()).isEqualTo(before);
    }

    @Test
    @WithMockUser(username = "w3-real-author")
    void validRecipientUsesAuthenticatedSenderAndPreservesEscapableText() throws Exception {
        createRecipient("w3-real-recipient", true);
        String text = "<script>alert('not executable')</script>";
        mvc.perform(send("w3-real-recipient", text))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("w3-real-author"))
                .andExpect(jsonPath("$.receiver").value("w3-real-recipient"))
                .andExpect(jsonPath("$.content").value(text))
                .andExpect(jsonPath("$.readAt").doesNotExist());
        var sent = messages.findThread("w3-real-author", "w3-real-recipient",
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getSender()).isEqualTo("w3-real-author");
        assertThat(sent.get(0).getId()).isNotEqualTo(999999L);
    }

    @Test
    @WithMockUser(username = "w3-csrf-sender")
    void signedInAccountCannotBypassCsrfEvenForValidRecipient() throws Exception {
        createRecipient("w3-csrf-recipient", true);
        mvc.perform(post("/chat/send").contentType(MediaType.APPLICATION_JSON)
                .content("{\"receiver\":\"w3-csrf-recipient\",\"content\":\"message\"}"))
                .andExpect(status().isForbidden());
    }
}
