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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:chatContactsApi27;DB_CLOSE_DELAY=-1")
class ChatContactsApiIntegrationTest {
    @Autowired ChatService chat;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="contacts-api-alice")
    void exposesOnlyViewerContactsWithUnreadCountsAndNoCache() throws Exception {
        chat.saveMessage(new ChatMessage("contacts-api-bob", "contacts-api-alice","message 1"));
        chat.saveMessage(new ChatMessage("contacts-api-bob", "contacts-api-alice","message 2"));
        chat.saveMessage(new ChatMessage("contacts-api-alice","contacts-api-charlie","outgoing"));
        chat.saveMessage(new ChatMessage("contacts-api-private","contacts-api-stranger","hidden"));
        mvc.perform(get("/chat/contacts"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].contact").value("contacts-api-bob"))
            .andExpect(jsonPath("$.content[0].unreadCount").value(2))
            .andExpect(jsonPath("$.content[1].contact").value("contacts-api-charlie"))
            .andExpect(jsonPath("$.content[1].unreadCount").value(0))
            .andExpect(content().string(not(containsString("contacts-api-private"))));
        mvc.perform(get("/chat/contacts").param("q","charlie"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].contact").value("contacts-api-charlie"));
        mvc.perform(get("/chat/contacts").with(user("contacts-api-outsider")))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/chat/contacts").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/chat/contacts").param("q","bad%filter"))
            .andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotReadConversationIndex() throws Exception {
        mvc.perform(get("/chat/contacts"))
            .andExpect(status().is3xxRedirection());
    }
}
