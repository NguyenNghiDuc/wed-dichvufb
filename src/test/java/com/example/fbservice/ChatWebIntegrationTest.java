package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:chatwebtest;DB_CLOSE_DELAY=-1")
class ChatWebIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ChatService chat;
    @Autowired UserRepository users;
    @Autowired com.example.fbservice.repository.ChatMessageRepository messages;

    private User createUser(String username, String role) {
        User user=new User();
        user.setUsername(username);
        user.setRole(role);
        user.setEnabled(true);
        user.setPassword("$2a$10$testHashOnlyNotRealCredentials");
        return users.save(user);
    }

    @Test @WithMockUser(username="webchat-customer")
    void rendersEscapedOwnerScopedMessagesAndLimitsPagination() throws Exception {
        createUser("webchat-staff","STAFF");
        chat.saveMessage(new ChatMessage("webchat-staff","webchat-customer",
            "<img src=x onerror=alert(1)>"));
        chat.saveMessage(new ChatMessage("webchat-staff","webchat-other",
            "private-different-customer"));
        for(int i=0;i<25;i++) chat.saveMessage(new ChatMessage(
            "webchat-customer","webchat-staff","safe-message-"+i));

        mvc.perform(get("/chat/ui/webchat-staff"))
            .andExpect(status().isOk())
            .andExpect(view().name("chat_thread"))
            .andExpect(model().attribute("messages",hasSize(20)))
            .andExpect(model().attribute("hasNext",true))
            .andExpect(content().string(not(containsString("private-different-customer"))));
        // Newest-first paging places the malicious *older* message on page 2,
        // so assert escaped HTML on the page containing that exact payload.
        mvc.perform(get("/chat/ui/webchat-staff").param("page","1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("messages",hasSize(6)))
            .andExpect(content().string(containsString("&lt;img")))
            .andExpect(content().string(not(containsString("<img src=x onerror=alert(1)>"))))
            .andExpect(content().string(not(containsString("private-different-customer"))));
        mvc.perform(get("/chat/ui/webchat-staff").param("page","10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/chat/ui/unknown-webchat-user"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/chat/ui/webchat-staff").with(user("webchat-outsider")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("messages",hasSize(0)));
    }

    @Test @WithMockUser(username="webchat-sender")
    void formRequiresCsrfAndDoesNotAcceptForgedSender() throws Exception {
        createUser("webchat-target","STAFF");
        mvc.perform(get("/chat/ui"))
            .andExpect(status().isOk())
            .andExpect(view().name("chat_contacts"))
            .andExpect(content().string(containsString("webchat-target")));
        mvc.perform(post("/chat/ui/webchat-target/send")
             .param("content","valid message"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/chat/ui/webchat-target/send").with(csrf())
             .param("content","valid message").param("sender","webchat-forged"))
            .andExpect(status().is3xxRedirection());
        assertThat(chat.threadFor("webchat-sender","webchat-target",0,20).getContent().get(0)
            .getSender()).isEqualTo("webchat-sender");
        mvc.perform(post("/chat/ui/webchat-target/send").with(csrf())
             .param("content","x".repeat(256))).andExpect(status().isBadRequest());
        mvc.perform(post("/chat/ui/webchat-target/read"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/chat/ui/webchat-target/read").with(csrf()))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/chat/ui/open").param("contact","webchat-target"))
            .andExpect(status().is3xxRedirection());
    }

    @Test @WithMockUser(username="webchat-read-owner")
    void singleMessageReadButtonCannotAcknowledgeOtherContactsOrTenants() throws Exception {
        createUser("webchat-read-contact", "STAFF");
        createUser("webchat-other-contact", "STAFF");
        ChatMessage incoming = chat.saveMessage(new ChatMessage(
                "webchat-read-contact", "webchat-read-owner", "Private incoming"));
        ChatMessage other = chat.saveMessage(new ChatMessage(
                "webchat-other-contact", "webchat-read-owner", "Different contact"));
        String valid = "/chat/ui/webchat-read-contact/messages/" + incoming.getId() + "/read";
        mvc.perform(get("/chat/ui/webchat-read-contact"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Đánh dấu tin này đã đọc")));
        mvc.perform(post(valid)).andExpect(status().isForbidden());
        mvc.perform(post(valid).with(user("webchat-outsider")).with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(messages.findById(incoming.getId()).orElseThrow().getReadAt()).isNull();
        mvc.perform(post("/chat/ui/webchat-read-contact/messages/" + other.getId() + "/read")
                .with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(messages.findById(other.getId()).orElseThrow().getReadAt()).isNull();
        mvc.perform(post(valid).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(messages.findById(incoming.getId()).orElseThrow().getReadAt()).isNotNull();
        mvc.perform(get("/chat/ui/webchat-read-contact"))
            .andExpect(content().string(not(containsString("Đánh dấu tin này đã đọc"))));
    }

    @Test void anonymousMayNotReadOrSendChat() throws Exception {
        mvc.perform(get("/chat/ui")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/chat/ui/anyuser/send").with(csrf()).param("content","hello"))
            .andExpect(status().is3xxRedirection());
    }
}
