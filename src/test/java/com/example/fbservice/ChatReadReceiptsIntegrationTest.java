package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.model.User;
import com.example.fbservice.notification.AccountNotificationRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:chatReadReceipts;DB_CLOSE_DELAY=-1")
class ChatReadReceiptsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ChatService chat;
    @Autowired UserRepository users;
    @Autowired AccountNotificationRepository notifications;

    @Test @WithMockUser(username="chat-read-alice")
    void onlyRecipientCanMarkTheirIncomingMessagesReadAndRepeatIsSafe() throws Exception {
        chat.saveMessage(new ChatMessage("chat-read-bob","chat-read-alice","Hi there"));
        chat.saveMessage(new ChatMessage("chat-read-alice","chat-read-bob","Reply"));
        chat.saveMessage(new ChatMessage("chat-read-bob","chat-read-charlie","Private"));
        mvc.perform(post("/chat/thread/chat-read-bob/read"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/chat/thread/chat-read-bob/read").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.marked").value(1));
        mvc.perform(post("/chat/thread/chat-read-bob/read").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.marked").value(0));
        mvc.perform(get("/chat/thread/chat-read-bob"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[?(@.receiver=='chat-read-alice')].readAt").exists());
        mvc.perform(post("/chat/thread/chat-read-bob/read").with(user("chat-read-outsider")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.marked").value(0));
        mvc.perform(post("/chat/thread/no/read").with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username="chat-notifier")
    void sendingToRealAccountCreatesNoticeWithoutEchoingUntrustedMessageBody() throws Exception {
        User recipient=new User();
        recipient.setUsername("chat-notice-recipient");
        recipient.setRole("USER");
        recipient.setEnabled(true);
        recipient.setPassword("$2a$10$dummyPasswordHashOnlyForTest");
        users.save(recipient);
        String payload="{\"sender\":\"spoof\",\"receiver\":\"chat-notice-recipient\","+
            "\"content\":\"<script>alert(1)</script>\"}";
        mvc.perform(post("/chat/send").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sender").value("chat-notifier"))
            .andExpect(jsonPath("$.readAt").doesNotExist());
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("chat-notice-recipient")).isEqualTo(1);
        var notice=notifications.findTop100ByOwnerUsernameOrderByCreatedAtDesc("chat-notice-recipient").get(0);
        assertThat(notice.getMessage()).doesNotContain("<script>");
    }
}
