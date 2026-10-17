package com.example.fbservice;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:unreadCounts27;DB_CLOSE_DELAY=-1")
class ChatUnreadCountsIntegrationTest {
    @Autowired ChatService chat;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="unread-index-alice")
    void countsUnreadOnlyForRecipientAndOnlyDisplayedContacts() throws Exception {
        chat.saveMessage(new ChatMessage("unread-index-bob","unread-index-alice","message one"));
        chat.saveMessage(new ChatMessage("unread-index-bob","unread-index-alice","message two"));
        chat.saveMessage(new ChatMessage("unread-index-alice","unread-index-bob","outgoing"));
        chat.saveMessage(new ChatMessage("unread-index-charlie","unread-index-alice","other contact"));
        chat.saveMessage(new ChatMessage("unread-index-bob","unread-index-outsider","private"));
        var counts=chat.unreadCountsForContacts("unread-index-alice",
                List.of("unread-index-bob","unread-index-charlie"));
        assertThat(counts).containsEntry("unread-index-bob",2L)
                          .containsEntry("unread-index-charlie",1L);
        mvc.perform(get("/chat/ui"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("2 chưa đọc")))
            .andExpect(content().string(containsString("1 chưa đọc")))
            .andExpect(content().string(not(containsString("unread-index-outsider"))));
        assertThat(chat.unreadCountsForContacts("unread-index-outsider",
                List.of("unread-index-bob")).get("unread-index-bob")).isZero();
        chat.markThreadRead("unread-index-alice","unread-index-bob");
        assertThat(chat.unreadCountsForContacts("unread-index-alice",
                List.of("unread-index-bob")).get("unread-index-bob")).isZero();
        assertThatThrownBy(() -> chat.unreadCountsForContacts("unread-index-alice",
                java.util.Collections.nCopies(21, "unread-index-bob")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
