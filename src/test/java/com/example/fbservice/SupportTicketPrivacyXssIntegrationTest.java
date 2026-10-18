package com.example.fbservice;

import com.example.fbservice.support.SupportTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w3ticketprivacyxss;DB_CLOSE_DELAY=-1")
class SupportTicketPrivacyXssIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;

    @Test
    @WithMockUser(username = "w3-ticket-owner")
    void neverShowsOrMutatesAnotherCustomersTicketAndEscapesHtml() throws Exception {
        var mine = tickets.create("w3-ticket-owner",
                "<img src=x onerror=alert(1)>",
                "<script>alert('private')</script> This is a support request.");
        var other = tickets.create("w3-other-owner", "Only another customer",
                "A secret description that must never appear to the owner above.");
        tickets.replyAsOwner("w3-ticket-owner", mine.getId(),
                "<script>alert('reply')</script>");
        long beforeOther = tickets.pageMessagesForOwner(
                "w3-other-owner", other.getId(), 0).getTotalElements();

        mvc.perform(get("/support/" + mine.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("&lt;img")))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert('private')</script>"))));
        mvc.perform(get("/support"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Only another customer"))));
        mvc.perform(get("/support/" + other.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/support/" + other.getId() + "/messages"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/support/" + other.getId() + "/reply").with(csrf())
                .param("message", "Attempt to change someone else's ticket"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/support/" + mine.getId() + "/reply")
                .param("message", "missing csrf")).andExpect(status().isForbidden());
        assertThat(tickets.pageMessagesForOwner("w3-other-owner",
                other.getId(), 0).getTotalElements()).isEqualTo(beforeOther);
    }
}
