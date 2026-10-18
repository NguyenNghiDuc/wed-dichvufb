package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:chatpagination;DB_CLOSE_DELAY=-1")
class ChatPaginationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ChatService chat;
    @Autowired UserRepository users;

    @Test
    @WithMockUser(username = "chat-alice")
    void threadShowsOnlyTwoParticipantsBothDirectionsWithBoundedPaging() throws Exception {
        chat.saveMessage(new ChatMessage("chat-alice", "chat-bob", "msg-alpha"));
        chat.saveMessage(new ChatMessage("chat-bob", "chat-alice", "msg-beta"));
        chat.saveMessage(new ChatMessage("chat-bob", "chat-charlie", "private-other"));
        chat.saveMessage(new ChatMessage("chat-dave", "chat-bob", "private-third-party"));

        mvc.perform(get("/chat/thread/chat-bob").param("page", "0").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(get("/chat/thread/chat-bob").param("page", "0").param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(content().string(not(containsString("private-other"))))
            .andExpect(content().string(not(containsString("private-third-party"))));

        mvc.perform(get("/chat/thread/chat-bob").with(user("chat-outsider")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content", empty()));
        mvc.perform(get("/chat/conversation/chat-alice/chat-bob").with(user("chat-outsider")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/chat/inbox/chat-bob"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chat-validation")
    void invalidParticipantsAndPagingReturn400() throws Exception {
        mvc.perform(get("/chat/thread/a")).andExpect(status().isBadRequest());
        mvc.perform(get("/chat/thread/chat-bob").param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/chat/thread/chat-bob").param("page", "10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/chat/thread/chat-bob").param("size", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/chat/thread/chat-bob").param("size", "51"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/chat/thread/chat-bob").param("size", "invalid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "chat-inbox")
    void legacyInboxHasHardFiftyMessageLimit() throws Exception {
        for (int i = 0; i < 55; i++) {
            chat.saveMessage(new ChatMessage("chat-sender", "chat-inbox", "message-" + i));
        }
        mvc.perform(get("/chat/inbox/chat-inbox")).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(50)));
        mvc.perform(get("/chat/conversation/chat-sender/chat-inbox"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(50)));
    }

    @Test
    @WithMockUser(username = "chat-verified-sender")
    void chatSendRejectsCsrfBypassAndIgnoresSpoofedSender() throws Exception {
        User recipient = new User();
        recipient.setUsername("chat-receiver");
        recipient.setRole("USER");
        recipient.setEnabled(true);
        recipient.setPassword("$2a$10$testOnlyReceiverPasswordHash");
        users.save(recipient);
        String body = """
            {"sender":"chat-spoof","receiver":"chat-receiver","content":"real message"}
            """;
        mvc.perform(post("/chat/send").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post("/chat/send").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sender").value("chat-verified-sender"));
        mvc.perform(get("/chat/thread/chat-receiver"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].sender").value("chat-verified-sender"));
    }

    @Test
    void anonymousCannotReadMessages() throws Exception {
        mvc.perform(get("/chat/thread/chat-bob")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/chat/inbox/chat-bob")).andExpect(status().is3xxRedirection());
    }
}
