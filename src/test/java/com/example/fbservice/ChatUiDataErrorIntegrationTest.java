package com.example.fbservice;

import com.example.fbservice.service.ChatService;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:w3chatdboutage;DB_CLOSE_DELAY=-1")
class ChatUiDataErrorIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @MockBean ChatService chats;

    @Test
    @WithMockUser(username = "private-chat-viewer")
    void transientInboxQueryFailureRendersSafe503WithNoStoreAndRetry() throws Exception {
        String sensitive = "password=private-database-value";
        when(chats.contactNamesFor("private-chat-viewer", null, 0))
                .thenThrow(new DataAccessResourceFailureException(sensitive));

        mvc.perform(get("/chat/ui"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(view().name("chat_error"))
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(content().string(containsString("Không thể tải hội thoại")))
            .andExpect(content().string(containsString("Thử lại")))
            .andExpect(content().string(not(containsString(sensitive))))
            .andExpect(content().string(not(containsString("private-chat-viewer"))));
    }

    @Test
    @WithMockUser(username = "private-chat-viewer")
    void privateThreadFailureAlsoShowsRedacted503InsteadOfAStackTrace() throws Exception {
        User recipient = new User();
        recipient.setUsername("private-chat-contact");
        recipient.setRole("STAFF");
        recipient.setEnabled(true);
        recipient.setPassword("$2a$10$testOnlyHashNoCredentials");
        users.saveAndFlush(recipient);
        String privateDetails = "jdbc:postgresql://private-host/secret-database";
        when(chats.threadFor("private-chat-viewer", "private-chat-contact", 0, 20))
                .thenThrow(new DataAccessResourceFailureException(privateDetails));

        mvc.perform(get("/chat/ui/private-chat-contact"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(view().name("chat_error"))
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(content().string(containsString("Trung tâm hỗ trợ")))
            .andExpect(content().string(not(containsString(privateDetails))))
            .andExpect(content().string(not(containsString("private-chat-contact"))));
    }

    @Test
    @WithMockUser(username = "private-chat-invalid")
    void invalidPagingStillReturns400RatherThanA503Fallback() throws Exception {
        when(chats.contactNamesFor("private-chat-invalid", null, -1))
                .thenThrow(new IllegalArgumentException("invalid paging"));
        mvc.perform(get("/chat/ui").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousStillCannotAccessChatErrorFallbackThroughTheInbox() throws Exception {
        mvc.perform(get("/chat/ui"))
                .andExpect(status().is3xxRedirection());
    }
}
